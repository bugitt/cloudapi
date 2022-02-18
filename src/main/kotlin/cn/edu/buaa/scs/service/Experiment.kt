package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.Experiment
import cn.edu.buaa.scs.model.assignments
import cn.edu.buaa.scs.model.experiments
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.user
import io.ktor.application.*
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.isNotNull
import org.ktorm.dsl.notEq
import org.ktorm.entity.count
import org.ktorm.entity.find

val ApplicationCall.experiment get() = ExperimentService(this)

class ExperimentService(val call: ApplicationCall) {
    fun get(id: Int): Experiment {
        val experiment = Experiment.id(id)
        call.user().assertRead(experiment)
        return experiment
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