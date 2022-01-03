package cn.edu.buaa.scs.utils.test

import cn.edu.buaa.scs.extensions.info
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.test() {
    // just for test
    route("/test") {
        get {
//            repeat(1000) {
//                launch(Dispatchers.Default) {
//                    delay(1000L)
//                    call.info("log in launch ${Thread.currentThread().name}")
//                }
//            }
            call.info("before respond")
            call.respond("ok")
        }
    }
}