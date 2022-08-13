package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.getOrPut
import cn.edu.buaa.scs.utils.schedule.CommonScheduler
import cn.edu.buaa.scs.utils.user
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

    fun getAllStudents(courseId: Int): List<User> {
        return mysql.courseStudents.filter { it.courseId eq courseId }.toList().map { it.student }
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