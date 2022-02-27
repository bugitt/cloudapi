package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.getOrPut
import cn.edu.buaa.scs.utils.schedule.CommonScheduler
import cn.edu.buaa.scs.utils.user
import io.ktor.application.*
import org.ktorm.dsl.eq
import org.ktorm.entity.count
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.toList
import java.util.concurrent.ConcurrentHashMap

val ApplicationCall.course get() = CourseService(this)

class CourseService(val call: ApplicationCall) {
    private val studentCntMap: MutableMap<Int, Int> = ConcurrentHashMap()

    fun studentCnt(courseId: Int): Int {
        return studentCntMap.getOrPut(courseId) {
            mysql.courseStudents.filter { it.courseId eq courseId }.count()
        }
    }

    data class StatCourseExps(
        val course: Course,
        val teacher: User,
        val students: List<User>,
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

    suspend fun statCourseExps(courseId: Int): StatCourseExps {
        val course = Course.id(courseId)
        val teacher = course.teacher
        val students = getAllStudents(courseId)
        val exps = mysql.experiments.filter { it.courseId eq course.id }.toList().sortedBy { it.startTime }
        val expDetails = CommonScheduler.multiCoroutinesProduce(exps.map { { call.experiment.statExp(it) } })
        return StatCourseExps(course, teacher, students, expDetails)
    }
}

fun Course.Companion.id(id: Int): Course {
    return mysql.courses.find { it.id eq id }
        ?: throw BusinessException("find course($id) from database error")
}