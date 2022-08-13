package cn.edu.buaa.scs.auth

import cn.edu.buaa.scs.model.Authentication
import cn.edu.buaa.scs.utils.token
import cn.edu.buaa.scs.utils.user
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoute() {
    route("/authentications") {
        get {
            call.respond(listOf(Authentication(call.user().id.lowercase(), call.token())))
        }
    }
}