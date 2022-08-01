package cn.edu.buaa.scs.controller

import cn.edu.buaa.scs.controller.plugins.*
import io.ktor.application.*

@Suppress("unused")
fun Application.webModule() {
    configureCORS()
    configureCallID()
    configureMonitoring()
    configureContentNegotiation()
    configureStatusPage()
    configureRouting()
    configureWebsocket()
}