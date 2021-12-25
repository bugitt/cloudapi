package cn.edu.buaa.scs.utils.test

import cn.edu.buaa.scs.extensions.info
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Route.test() {
    // just for test
    route("/test") {
        get {
            launch {
                delay(10000L)
                call.info("log in launch")
                throw Exception("just for fun")
            }
            call.info("before respond")
            call.respond("ok")
        }
    }
}