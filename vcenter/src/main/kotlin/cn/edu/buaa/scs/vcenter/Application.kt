package cn.edu.buaa.scs.vcenter

import cn.edu.buaa.scs.config.globalConfig
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
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

val escapeApiMap = mapOf(
    "/api/v2/vcenter/health" to listOf(HttpMethod.Get)
)

fun Application.auth() {
    install(createApplicationPlugin("auth") {
        on(CallSetup) { call ->
            if (escapeApiMap[call.request.path()]?.contains(call.request.httpMethod) == true) {
                return@on
            }
            val token = call.request.headers[HttpHeaders.Authorization].orEmpty().removePrefix("Bearer ")
            if (token != globalConfig.vcenter.serviceToken) {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    })
}
