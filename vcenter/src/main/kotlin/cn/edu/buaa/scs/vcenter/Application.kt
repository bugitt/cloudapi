package cn.edu.buaa.scs.vcenter

import cn.edu.buaa.scs.utils.Constants
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import org.ktorm.jackson.KtormModule

fun main(args: Array<String>) {
    VCenterWrapper.initialize()
    embeddedServer(Netty, port = Constants.VCenter.port, host = "0.0.0.0", module = Application::module)
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
    configureContentNegotiation()
    vcenterRouting()
}
