package cn.edu.buaa.scs.controller.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*

fun Application.configureWebsocket() {
    install(WebSockets)
}