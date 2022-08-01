package cn.edu.buaa.scs.controller.plugins

import io.ktor.application.*
import io.ktor.websocket.*

fun Application.configureWebsocket() {
    install(WebSockets)
}