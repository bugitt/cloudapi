package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.Course
import cn.edu.buaa.scs.model.Experiment
import cn.edu.buaa.scs.model.experiments
import cn.edu.buaa.scs.storage.mysql
import org.ktorm.dsl.eq
import org.ktorm.entity.find

fun Experiment.Companion.id(id: Int): Experiment {
    return mysql.experiments.find { it.id eq id }
        ?: throw BusinessException("find experiment($id) from database error")
}

fun Experiment.getCourse(): Course = Course.id(this.courseId)