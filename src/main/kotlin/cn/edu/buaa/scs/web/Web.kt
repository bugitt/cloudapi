package cn.edu.buaa.scs.web

import cn.edu.buaa.scs.web.plugins.*
import io.ktor.application.*

@Suppress("unused")
fun Application.webModule() {
    configureCORS()
    configureCallID()
    configureMonitoring()
    configureContentNegotiation()
    configureStatusPage()
    configureRouting()
}