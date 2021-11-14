package cn.edu.buaa.scs.plugins

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
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