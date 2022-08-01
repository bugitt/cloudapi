package cn.edu.buaa.scs.controller

import cn.edu.buaa.scs.controller.plugins.*
import io.ktor.application.*

@Suppress("unused")
fun Application.webModule() {
    configureWebsocket()
    configureCORS()
    configureCallID()
    configureMonitoring()
    configureContentNegotiation()
    configureStatusPage()
    configureRouting()
}