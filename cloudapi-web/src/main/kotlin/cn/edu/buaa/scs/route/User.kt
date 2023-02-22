package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.SimpleUser
import cn.edu.buaa.scs.controller.models.UserModel
import cn.edu.buaa.scs.model.User
import cn.edu.buaa.scs.service.hr
import cn.edu.buaa.scs.service.id
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoute() {
    route("/students") {
        get {
            val search = call.parameters["search"]
            call.respond(call.hr.getStudents(search).map { convertUserModel(it) })
        }
    }

    route("/stuffs") {
        get {
            val search = call.parameters["search"]
            call.respond(call.hr.getTeachersAndStudents(search).map { convertUserModel(it) })
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
