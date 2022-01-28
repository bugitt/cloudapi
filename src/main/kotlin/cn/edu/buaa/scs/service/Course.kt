package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.Course
import cn.edu.buaa.scs.model.courses
import cn.edu.buaa.scs.storage.mysql
import org.ktorm.dsl.eq
import org.ktorm.entity.find

fun Course.Companion.id(id: Int): Course {
    return mysql.courses.find { it.id eq id }
        ?: throw BusinessException("find course($id) from database error")
}