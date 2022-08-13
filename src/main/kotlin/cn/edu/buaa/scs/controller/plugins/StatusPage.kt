package cn.edu.buaa.scs.controller.plugins

import cn.edu.buaa.scs.error.AuthenticationException
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.utils.error
import com.google.common.base.Throwables
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPage() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.error(Throwables.getStackTraceAsString(cause))
            when (cause) {

                // 400 参数错误
                is BadRequestException -> call.respond(
                    HttpStatusCode.BadRequest,
                    cause.message.toString().ifEmpty { "" })

                // 401 需要鉴权
                is AuthenticationException -> call.respond(
                    HttpStatusCode.Unauthorized,
                    cause.message.toString().ifEmpty { "need token" })

                // 403 无权限 可能是token错误，也可能就是没权限
                is AuthorizationException -> call.respond(
                    HttpStatusCode.Forbidden,
                    cause.message.toString()
                        .ifEmpty { "It may be a token error. It may also be that you do not have permission to access the resource." }
                )

                // 404
                is NotFoundException, is cn.edu.buaa.scs.error.NotFoundException -> call.respond(
                    HttpStatusCode.NotFound,
                    cause.message.toString().ifEmpty { "resource not found" }
                )

                // 500
                else -> call.respond(HttpStatusCode.InternalServerError, cause.message.toString())
            }
        }
    }
}
