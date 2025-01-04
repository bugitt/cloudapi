package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.model.User
import cn.edu.buaa.scs.model.UserRole
import cn.edu.buaa.scs.model.Users
import cn.edu.buaa.scs.model.users
import cn.edu.buaa.scs.utils.user
import io.ktor.server.application.*
import cn.edu.buaa.scs.storage.mysql
import org.ktorm.dsl.delete
import org.ktorm.dsl.inList

val ApplicationCall.admin: AdminService
    get() = AdminService.getSvc(this) { AdminService(this) }

class AdminService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<AdminService>()

    fun addUser(id: String, name: String?, role: UserRole, departmentId: Int): User {
        if (!call.user().isAdmin()) {
            throw AuthorizationException("only admin can add user")
        }

        return User.createNewUnActiveUser(id, name, role, departmentId)
    }

    fun deleteUsers(userIds: List<String>) {
        mysql.delete(Users) {
            it.id inList userIds
        }
    }
}
