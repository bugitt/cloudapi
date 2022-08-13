package cn.edu.buaa.scs.controller.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*
import java.util.*

fun Application.configureCallID() {
    install(CallId) {

        retrieve { call ->
            call.request.header(HttpHeaders.XRequestId)
        }

        generate { UUID.randomUUID().toString() }

        // 校验 callID
        verify { callId: String ->
            callId.isNotEmpty()
        }

        // 设置 request 和 response 的 header
        header(HttpHeaders.XRequestId)
    }
}