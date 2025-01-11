package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.CreateUserRequest
import cn.edu.buaa.scs.controller.models.DeleteAdminUserRequest
import cn.edu.buaa.scs.model.UserRole
import cn.edu.buaa.scs.service.admin
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.adminRoute() {
    route("/admin") {
        route("/user") {
            post {
                val req = call.receive<CreateUserRequest>()
                val role = UserRole.fromLevel(req.role)
                val name = req.name
                val email = req.email
                val departmentId = req.departmentId
                call.respond(convertUserModel(call.admin.addUser(req.id, name, role, email, departmentId)))
            }

            delete {
                val req = call.receive<DeleteAdminUserRequest>()
                val userIds = req.userIds
                call.admin.deleteUsers(userIds)
                call.respond("OK")
            }
        }
    }
}
