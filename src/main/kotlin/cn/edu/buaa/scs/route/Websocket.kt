package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.service.keepalive
import cn.edu.buaa.scs.service.sshWS
import io.ktor.features.*
import io.ktor.routing.*
import io.ktor.websocket.*

fun Route.websocketRoute() {
    route("/ws") {
        webSocket("/ssh/{uuid}/{token}") {
            keepalive {
                sshWS(call.parameters["uuid"] ?: throw BadRequestException("uuid is required"))
            }
        }
    }
}