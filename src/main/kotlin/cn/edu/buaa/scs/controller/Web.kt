package cn.edu.buaa.scs.controller

import cn.edu.buaa.scs.controller.plugins.*
import io.ktor.server.application.*

@Suppress("unused")
fun Application.webModule() {
    configureAuth()
    configureWebsocket()
    configureCORS()
    configureCallID()
    configureMonitoring()
    configureContentNegotiation()
    configureStatusPage()
    configureRouting()
}
