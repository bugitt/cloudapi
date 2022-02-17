package cn.edu.buaa.scs.controller.plugins

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*

fun Application.configureCORS() {
    install(CORS) {
        anyHost()

        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Put)
        method(HttpMethod.Patch)
        method(HttpMethod.Delete)

        header(HttpHeaders.ContentType)
        header(HttpHeaders.Authorization)
        allowHeadersPrefixed("X-Request-")
        allowHeadersPrefixed("x-scs-")
    }
}