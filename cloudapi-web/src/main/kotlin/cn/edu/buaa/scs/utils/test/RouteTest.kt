package cn.edu.buaa.scs.utils.test

import cn.edu.buaa.scs.service.auth
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.test() {
    // just for test
    route("/test") {
        get {
            call.auth.sendActiveEmail("99141002", "李", "loheagn@icloud.com")

            call.respondText("Hello, world!")
        }
    }
}
