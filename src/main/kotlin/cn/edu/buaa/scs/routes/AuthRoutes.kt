package cn.edu.buaa.scs.routes

import cn.edu.buaa.scs.extensions.token
import cn.edu.buaa.scs.extensions.user
import cn.edu.buaa.scs.model.Authentication
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.authRoute() {
    route("/authentications") {
        get {
            call.respond(listOf(Authentication(call.user().id, call.token())))
        }
    }
}