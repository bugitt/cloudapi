package cn.edu.buaa.scs.controller

import cn.edu.buaa.scs.controller.plugins.*
import io.ktor.server.application.*

@Suppress("unused")
fun Application.webModule() {
    configureMonitoring()
    configureAuth()
    configureWebsocket()
    configureCORS()
    configureCallID()
    configureContentNegotiation()
    configureStatusPage()
    configureRouting()
}
