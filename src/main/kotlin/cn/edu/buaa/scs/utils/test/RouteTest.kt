package cn.edu.buaa.scs.utils.test

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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


//            call.info("before withContext, ${Thread.currentThread().id}")
//            withContext(Dispatchers.IO) {
//                delay(100L)
//                call.info("delay end, ${Thread.currentThread().id}")
//                withContext(Dispatchers.IO) {
//                    delay(100L)
//                    call.info("delay end, ${Thread.currentThread().id}")
//                }
//            }
//            call.info("before respond")
//            withContext(Dispatchers.Default) {
//                val r1 = async {
//                    delay(1000L)
//                    call.info("r1")
//                    "r1"
//                }
//                val r2 = async {
//                    delay(10L)
//                    call.info("r2")
//                    "r2"
//                }
//                call.info("${r1.await()}, ${r2.await()}")
//            }

            withContext(Dispatchers.IO) {
                delay(10000L)
            }

            call.respond("ok")
        }
    }
}