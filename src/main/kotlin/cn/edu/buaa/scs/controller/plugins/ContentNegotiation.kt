package cn.edu.buaa.scs.controller.plugins

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import org.ktorm.jackson.KtormModule

fun Application.configureContentNegotiation() {
    install(ContentNegotiation) {
        jackson {
            registerModule(KtormModule())
        }
    }
}