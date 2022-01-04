package cn.edu.buaa.scs.utils

import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.model.User
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.request.*

fun ApplicationCall.token(): String =
    this.attributes[TOKEN_KEY]

fun ApplicationCall.user(): User =
    this.attributes[USER_KEY]

fun ApplicationCall.userId(): String =
    this.attributes[USER_ID_KEY]

fun ApplicationCall.assertPermission(boolean: Boolean, action: () -> Unit) = run {
    if (boolean) return@run

    try {
        action()
    } catch (e: AuthorizationException) {
        throw e
    }
}

val ApplicationCall.baseURLName: String
    get() = this.request.uri.substringBefore("?").split("/").last()

@Suppress("unused")
fun ApplicationCall.trance(msg: String) {
    logger(baseURLName)().trace { "[${request.uri}] -  [$callId] - \t$msg" }
}

@Suppress("unused")
fun ApplicationCall.info(msg: String) {
    logger(baseURLName)().info { "[${request.uri}] -  [$callId] - \t$msg" }
}

@Suppress("unused")
fun ApplicationCall.debug(msg: String) {
    logger(baseURLName)().debug { "[${request.uri}] -  [$callId] - \t$msg" }
}

@Suppress("unused")
fun ApplicationCall.warn(msg: String) {
    logger(baseURLName)().warn { "[${request.uri}] -  [$callId] - \t$msg" }
}

@Suppress("unused")
fun ApplicationCall.error(msg: String) {
    logger(baseURLName)().error { "[${request.uri}] -  [$callId] - \t$msg" }
}