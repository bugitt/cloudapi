package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.model.User
import cn.edu.buaa.scs.model.UserRole
import cn.edu.buaa.scs.utils.user
import io.ktor.server.application.*

val ApplicationCall.admin: AdminService
    get() = AdminService.getSvc(this) { AdminService(this) }

class AdminService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<AdminService>()

    fun addUser(id: String, role: UserRole): User {
        if (!call.user().isAdmin()) {
            throw AuthorizationException("only admin can add user")
        }

        return User.createNewUnActiveUser(id, null, role)
    }
}
