package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.User
import cn.edu.buaa.scs.model.UserRole
import cn.edu.buaa.scs.model.users
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.user
import io.ktor.server.application.*
import org.ktorm.dsl.*
import org.ktorm.entity.*

fun User.Companion.id(id: String): User =
    mysql.users.find { it.id eq id } ?: throw BusinessException("find user($id) from mysql error")


fun User.Companion.getUerListByIdList(idList: List<String>): List<User> {
    return if (idList.isEmpty()) listOf()
    else mysql.users.filter { it.id.inList(idList) }.toList()
}

val ApplicationCall.hr
    get() = UserService.getSvc(this) { UserService(this) }

class UserService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<UserService>()

    fun getStudents(search: String?, limit: Int = 10): List<User> {
        if (call.user().isStudent()) return listOf()

        if (search.isNullOrBlank()) return listOf()

        var query = mysql.users
            .filter {
                (it.id.like("%$search%").or(it.name.like("%$search%")))
                    .and(it.role eq UserRole.STUDENT)
            }
            .sortedBy { it.id }
        if (limit != -1) {
            query = query.take(limit)
        }
        return query.toList()
    }

    fun getTeachersAndStudents(search: String?, limit: Int = 10): List<User> {
        if (call.user().isStudent()) return listOf()

        if (search.isNullOrBlank()) return listOf()

        var query = mysql.users
            .filter {
                (it.id.like("%$search%").or(it.name.like("%$search%")))
                    .and((it.role eq UserRole.STUDENT).or(it.role eq UserRole.TEACHER))
            }
            .sortedBy { it.id }
        if (limit != -1) {
            query = query.take(limit)
        }
        return query.toList()
    }
}
