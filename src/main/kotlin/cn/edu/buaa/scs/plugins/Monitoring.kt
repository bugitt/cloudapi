package cn.edu.buaa.scs.plugins

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
        format { call ->
            val reqID = call.response.headers[HttpHeaders.XRequestId]
            val method = call.request.httpMethod.value
            val requestUrl = call.request.path()
            val version = call.request.httpVersion
            val status = call.response.status()
            val referrer = call.request.headers.let {
                if (it.contains("referrer")) it["referrer"] else it["referer"]
            }
            val userAgent = call.request.userAgent()
            "$reqID - \"$method $requestUrl $version\" $status \"$referrer\" \"$userAgent\""
        }
    }

}
