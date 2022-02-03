package cn.edu.buaa.scs.utils.test

import cn.edu.buaa.scs.utils.info
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
            call.info("before withContext, ${Thread.currentThread().id}")
            withContext(Dispatchers.IO) {
                delay(100L)
                call.info("delay end, ${Thread.currentThread().id}")
            }
            call.info("before respond")
            call.respond("ok")
        }
    }
}