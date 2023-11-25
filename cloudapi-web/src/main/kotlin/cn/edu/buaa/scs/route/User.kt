package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.ChangePasswordRequest
import cn.edu.buaa.scs.controller.models.SimpleUser
import cn.edu.buaa.scs.controller.models.UserModel
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.Department
import cn.edu.buaa.scs.model.User
import cn.edu.buaa.scs.model.id
import cn.edu.buaa.scs.service.id
import cn.edu.buaa.scs.service.userService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoute() {
    route("/students") {
        get {
            val search = call.parameters["search"]
            call.respond(call.userService.getStudents(search).map { convertUserModel(it) })
        }
    }

    route("/stuffs") {
        get {
            val search = call.parameters["search"]
            call.respond(call.userService.getTeachersAndStudents(search).map { convertUserModel(it) })
        }
    }

    route("/myAssistants") {
        get {
            call.respond(call.userService.myAssistants())
        }
    }

    route("/users") {
        route("/{userId}") {
            fun ApplicationCall.getUserIdFromPath(): String =
                parameters["userId"]
                    ?: throw BadRequestException("course id is invalid")

            patch {
                call.userService.patchUser(call.getUserIdFromPath(), call.receive())
                call.respond("OK")
            }

            patch("/changePassword") {
                val req = call.receive<ChangePasswordRequest>()
                call.userService.changePassword(call.getUserIdFromPath(), req.old, req.new)
                call.respond("OK")
            }
        }

    }
}

internal fun convertUserModel(user: User): UserModel {
    return UserModel(
        id = user.id,
        name = user.name,
        department = user.departmentId,
        email = user.email,
        role = user.role.name.lowercase(),
        departmentName = Department.id(user.departmentId).name,
    )
}

internal fun convertSimpleUser(userId: String): SimpleUser? {
    return try {
        if (userId == "admin") return SimpleUser("admin", "管理员")
        val user = User.id(userId)
        SimpleUser(user.id, user.name)
    } catch (e: Throwable) {
        null
    }
}
