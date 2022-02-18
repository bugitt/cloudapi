package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.user
import cn.edu.buaa.scs.utils.userId
import io.ktor.application.*
import org.ktorm.dsl.*
import org.ktorm.entity.count
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.toList

val ApplicationCall.experiment get() = ExperimentService(this)

class ExperimentService(val call: ApplicationCall) {
    fun get(id: Int): Experiment {
        val experiment = Experiment.id(id)
        call.user().assertRead(experiment)
        return experiment
    }

    fun getAll(termId: Int? = null, submitted: Boolean? = null): List<Experiment> {
        val courseQuery = if (termId != null && termId > 0) mysql.courseStudents.filter {
            val course = it.courseId.referenceTable as Courses
            course.termId.eq(termId)
        } else {
            mysql.courseStudents
        }
        val courseIdList = courseQuery.filter { it.studentId.eq(call.userId()) }.toList().map { it.courseId }
        if (courseIdList.isEmpty()) return emptyList()
        return if (submitted == null) {
            mysql.experiments.filter { it.courseId.inList(courseIdList) }.toList()
        } else if (submitted) {
            mysql.from(Experiments).leftJoin(Assignments, on = Experiments.id.eq(Assignments.expId)).select()
                .where(Assignments.fileId.notEq(0) and Assignments.fileId.isNotNull())
                .map { Experiments.createEntity(it) }
        } else {
            mysql.from(Experiments).leftJoin(Assignments, on = Experiments.id.eq(Assignments.expId)).select()
                .where(Assignments.fileId.eq(0) or Assignments.fileId.isNull())
                .map { Experiments.createEntity(it) }
        }
    }

    fun statExp(experiment: Experiment): CourseService.StatCourseExps.ExpDetail {
        // 统计已经交作业的人数
        val submittedAssignmentCnt =
            mysql.assignments.count { it.expId.eq(experiment.id) and it.fileId.isNotNull() and it.fileId.notEq(0) }
        // TODO: 统计虚拟机数量
        val vmCnt = 0
        return CourseService.StatCourseExps.ExpDetail(experiment, vmCnt, submittedAssignmentCnt)
    }
}

fun Experiment.Companion.id(id: Int): Experiment {
    return mysql.experiments.find { it.id eq id }
        ?: throw BusinessException("find experiment($id) from database error")
}