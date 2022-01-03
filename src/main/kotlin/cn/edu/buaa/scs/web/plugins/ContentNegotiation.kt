package cn.edu.buaa.scs.web.plugins

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import org.ktorm.jackson.KtormModule

fun Application.configureContentNegotiation() {
    install(ContentNegotiation) {
        jackson {
            registerModule(KtormModule())
        }
    }
}