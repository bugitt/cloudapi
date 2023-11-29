package cn.edu.buaa.scs.utils.test

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.test() {
    // just for test
    route("/test") {
        get {
            call.respondText("Hello, world!")
        }
    }
}
