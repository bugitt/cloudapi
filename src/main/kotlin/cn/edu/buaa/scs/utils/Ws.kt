package cn.edu.buaa.scs.utils

import io.ktor.websocket.*
import java.util.concurrent.atomic.AtomicInteger

class WsSessionHolder(val session: DefaultWebSocketSession) {
    companion object {
        val lastId = AtomicInteger(0)
    }

    val id = lastId.getAndIncrement()
}
