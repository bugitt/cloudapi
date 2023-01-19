package cn.edu.buaa.scs.controller.plugins

import cn.edu.buaa.scs.auth.fetchToken
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*

fun Application.configureAuth() {
    install(createApplicationPlugin("auth") {
        on(CallSetup) { call ->
            fetchToken(call)
        }
    })
}
