package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertAdmin
import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.*
import cn.edu.buaa.scs.utils.schedule.CommonScheduler
import cn.edu.buaa.scs.utils.schedule.forEachAsync
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ktorm.dsl.*
import org.ktorm.entity.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

val ApplicationCall.course
    get() = CourseService.getSvc(this) { CourseService(this) }

class CourseService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<CourseService>()

    private val studentCntMap: MutableMap<Int, Int> = ConcurrentHashMap()

    fun studentCnt(courseId: Int): Int {
        return studentCntMap.getOrPut(courseId) {
            mysql.courseStudents.filter { it.courseId eq courseId }.count()
        }
    }

    data class StatCourseExps(
        val course: Course,
        val teacher: User,
        val studentCnt: Int,
        val expDetails: List<ExpDetail>,
    ) {
        data class ExpDetail(
            val exp: Experiment,
            val vmCnt: Int,
            val submittedAssignmentsCnt: Int,
        )
    }

    fun addCourse(teacherId: String, courseName: String, termId: Int): Course {
        if (teacherId != call.userId() && !call.user().isAdmin()) {
            throw AuthorizationException("only admin or teacher can add course")
        }
        val teacher = User.id(teacherId)
        val term = Term.id(termId)
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ZoneId.systemDefault())
        val currentTimeFormatted = formatter.format(Instant.now())
        val course = Course {
            this.teacher = teacher
            this.name = courseName
            this.term = term
            this.departmentId = "21"
            this.createTime = currentTimeFormatted
            this.resourceFolder = ""
        }

        mysql.courses.add(course)

        return course
    }

    fun get(id: Int): Course {
        val course = Course.id(id)
        call.user().assertRead(course)
        return course
    }

    fun patch(id: Int, termId: Int, name: String): Course {
        val course = Course.id(id)
        call.user().assertWrite(course)

        val term = Term.id(termId)
        course.term = term
        course.name = name

        course.flushChanges()
        return course
    }

    fun delete(id: Int): Course {
        val course = Course.id(id)
        call.user().assertAdmin(course)

        course.delete()
        return course
    }

    fun getAllCourses(termId: Int? = null): List<Course> {
        if (call.user().isTeacher()) return getAllManagedCourses()

        var query = mysql.courses
        if (call.user().isStudent()) {
            val courseIdList = mysql.courseStudents.filter { it.studentId.eq(call.userId()) }.map { it.courseId }
            if (courseIdList.isEmpty()) return listOf()
            query = query.filter { it.id.inList(courseIdList) }
        }

        if (termId != null) {
            query = query.filter { it.termId.eq(termId) }
        }

        return query.toList().sortedByDescending { it.id }
    }

    fun getAllManagedCourses(): List<Course> {
        return if (call.user().isAdmin()) {
            getAllCourses()
        } else if (call.user().isTeacher()) {
            val courseIdList = mysql.assistants.filter { it.studentId.eq(call.userId()) }.map { it.courseId.toInt() }
            if (courseIdList.isEmpty()) mysql.courses.filter { it.teacherId.eq(call.userId()) }.toList().sortedByDescending { it.id }
            else mysql.courses.filter { it.teacherId.eq(call.userId()) or it.id.inList(courseIdList) }.toList().sortedByDescending { it.id }
        } else {
            val courseIdList = mysql.assistants.filter { it.studentId.eq(call.userId()) }.map { it.courseId.toInt() }
            if (courseIdList.isEmpty()) listOf()
            else mysql.courses.filter { it.id.inList(courseIdList) }.toList().sortedByDescending { it.id }
        }
    }

    fun getAllStudentsInternal(courseId: Int): List<User> {
        return mysql.courseStudents.filter { it.courseId eq courseId }.map { it.student }.toList()
    }

    fun getAllStudents(courseId: Int): List<User> {
        val course = Course.id(courseId)
        call.user().isCourseAdmin(course)
        return getAllStudentsInternal(courseId)
    }

    fun deleteStudents(courseId: Int, studentIdList: List<String>) {
        if (studentIdList.isEmpty()) {
            return
        }
        call.user().assertWrite(Course.id(courseId))

        mysql.delete(CourseStudents) {
            it.courseId.eq(courseId) and it.studentId.inList(studentIdList)
        }

    }

    fun addAssistant(courseId: Int, studentId: String) {
        val course = Course.id(courseId)
        call.user().assertWrite(course)

        val student = User.id(studentId)
        if (mysql.assistants.filter { it.courseId.eq(courseId.toString()).and(it.studentId eq studentId) }
                .count() > 0) {
            throw BusinessException("student $studentId already is assistant of course $courseId")
        }

        val currentTime = SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(System.currentTimeMillis())
        val assistant = Assistant {
            this.courseId = courseId.toString()
            this.studentId = studentId
            this.createTime = currentTime
        }

        mysql.assistants.add(assistant)
    }

    fun deleteAssistant(assistantId: Int) {
        val assistant = mysql.assistants.find { it.id.eq(assistantId) } ?: throw NotFoundException()
        val course = Course.id(assistant.courseId.toInt())
        call.user().assertAdmin(course)

        assistant.delete()
    }

    suspend fun addNewStudents(courseId: Int, studentIdList: List<String>) {
        if (studentIdList.isEmpty()) {
            return
        }
        call.user().assertWrite(Course.id(courseId))

        // make sure students exist
        studentIdList.forEachAsync { studentId ->
            if (!mysql.users.exists { it.id.inList(studentId.lowerUpperNormal()) }) {
                User.createNewUnActiveUser(studentId, null, UserRole.STUDENT, 0)
            }
        }

        val alreadyExistStudentIdList =
            mysql.courseStudents.filter { it.courseId.eq(courseId) and it.studentId.inList(studentIdList) }
                .map { it.student }.toList().map { it.id }.toSet()

        val newStudentList = mysql.users.filter {
            it.id.inList(
                studentIdList
                    .filterNot { id -> alreadyExistStudentIdList.contains(id) }
                    .flatMap { id -> id.lowerUpperNormal() }
            )
        }.toList()

        // add students
        mysql.batchInsert(CourseStudents) {
            newStudentList.forEach { student ->
                item {
                    set(it.studentId, student.id)
                    set(it.courseId, courseId)
                }
            }
        }
    }

    suspend fun statCourseExps(courseId: Int): StatCourseExps = withContext(Dispatchers.Default) {
        val course = Course.id(courseId)
        val teacher = course.teacher
        val exps = mysql.experiments.filter { it.courseId eq course.id }.toList().sortedBy { it.startTime }

        // 统计一下交作业的人数
        val assignmentMap =
            mysql.assignments.filter { it.courseId.eq(courseId) and it.fileId.isNotNull() and it.fileId.notEq(0) }
                .groupingBy { it.expId }.aggregateColumns { count(it.id) }

        val expDetails =
            CommonScheduler.multiCoroutinesProduceSync(exps.map {
                {
                    call.experiment.statExp(
                        it,
                        assignmentMap[it.id] ?: 0
                    )
                }
            }, Dispatchers.IO)
        StatCourseExps(course, teacher, call.course.studentCnt(courseId), expDetails)
    }
}

fun Course.Companion.id(id: Int): Course {
    return mysql.courses.find { it.id eq id }
        ?: throw BusinessException("find course($id) from database error")
}
