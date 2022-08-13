package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.service.keepalive
import cn.edu.buaa.scs.service.sshWS
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*

fun Route.websocketRoute() {
    route("/ws") {
        webSocket("/ssh/{uuid}/{token}") {
            keepalive {
                sshWS(call.parameters["uuid"] ?: throw BadRequestException("uuid is required"))
            }
        }
    }
}