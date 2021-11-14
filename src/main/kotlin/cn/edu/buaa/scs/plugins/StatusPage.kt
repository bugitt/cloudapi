package cn.edu.buaa.scs.plugins

import cn.edu.buaa.scs.error.AuthenticationException
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.extensions.error
import com.google.common.base.Throwables
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*

fun Application.configureStatusPage() {
    install(StatusPages) {
        exception<Throwable> { cause ->
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
