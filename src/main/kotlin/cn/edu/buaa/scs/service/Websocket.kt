package cn.edu.buaa.scs.service

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

suspend fun DefaultWebSocketServerSession.keepalive(action: suspend DefaultWebSocketServerSession.() -> Unit) {
    launch {
        while (true) {
            outgoing.send(Frame.Ping(ByteArray(0)))
            delay(50L)
        }
    }
    action()
}