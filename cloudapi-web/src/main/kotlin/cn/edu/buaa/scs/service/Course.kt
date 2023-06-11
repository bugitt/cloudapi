package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.error.BusinessException
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

    fun get(id: Int): Course {
        val course = Course.id(id)
        call.user().assertRead(course)
        return course
    }

    fun getAllCourses(): List<Course> {
        // just for admin by now

        if (!call.user().isAdmin()) {
            throw BusinessException("only admin can get all courses")
        }
        return mysql.courses.toList().sortedByDescending { it.id }
    }

    fun getAllManagedCourses(): List<Course> {
        return if (call.user().isAdmin()) {
            getAllCourses()
        } else if (call.user().isTeacher()) {
            mysql.courses.filter { it.teacherId.eq(call.userId()) }.toList()
        } else {
            val courseIdList = mysql.assistants.filter { it.studentId.eq(call.userId()) }.map { it.courseId.toInt() }
            if (courseIdList.isEmpty()) listOf()
            else mysql.courses.filter { it.id.inList(courseIdList) }.toList()
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

    suspend fun addNewStudents(courseId: Int, studentIdList: List<String>) {
        if (studentIdList.isEmpty()) {
            return
        }
        val course = Course.id(courseId)
        if (!call.user().isCourseAdmin(course)) {
            throw AuthorizationException("only course admin can add new students")
        }

        // make sure students exist
        studentIdList.forEachAsync { studentId ->
            if (!mysql.users.exists { it.id.inList(studentId.lowerUpperNormal()) }) {
                User.createNewUnActiveUser(studentId, null, UserRole.STUDENT)
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
