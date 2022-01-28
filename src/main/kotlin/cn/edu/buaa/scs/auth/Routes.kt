package cn.edu.buaa.scs.auth

import cn.edu.buaa.scs.model.Authentication
import cn.edu.buaa.scs.utils.token
import cn.edu.buaa.scs.utils.user
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.authRoute() {
    route("/authentications") {
        get {
            call.respond(listOf(Authentication(call.user().id.lowercase(), call.token())))
        }
    }
}