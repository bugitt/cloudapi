package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.User
import cn.edu.buaa.scs.model.users
import cn.edu.buaa.scs.storage.mysql
import org.ktorm.dsl.eq
import org.ktorm.entity.find

fun User.Companion.id(id: String): User =
    mysql.users.find { it.id eq id } ?: throw BusinessException("find user($id) from mysql error")