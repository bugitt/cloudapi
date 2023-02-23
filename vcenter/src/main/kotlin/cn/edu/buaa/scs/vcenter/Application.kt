package cn.edu.buaa.scs.vcenter

import cn.edu.buaa.scs.config.globalConfig
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import org.ktorm.jackson.KtormModule

fun main() {
    VCenterWrapper.initialize()
    embeddedServer(Netty, port = globalConfig.vcenter.port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}


fun Application.configureContentNegotiation() {
    install(ContentNegotiation) {
        jackson {
            registerModule(KtormModule())
        }
    }
}

fun Application.module() {
    auth()
    configureContentNegotiation()
    vcenterRouting()
}

fun Application.auth() {
    install(createApplicationPlugin("auth") {
        on(CallSetup) { call ->
            val token = call.request.headers[HttpHeaders.Authorization].orEmpty().removePrefix("Bearer ")
            if (token != globalConfig.vcenter.serviceToken) {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    })
}
