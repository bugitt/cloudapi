@file:Suppress("unused")

package cn.edu.buaa.scs.utils

import cn.edu.buaa.scs.model.User
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*

fun ApplicationCall.headerString(key: String): String =
    this.request.header(key) ?: throw BadRequestException("missing header: $key")

fun ApplicationCall.headerInt(key: String): Int =
    this.request.header(key)?.toInt() ?: throw BadRequestException("missing header: $key")

fun ApplicationCall.headerIntOrNull(key: String): Int? =
    this.request.header(key)?.toInt()


fun ApplicationCall.token(): String =
    this.attributes[TOKEN_KEY]

fun ApplicationCall.user(): User =
    this.attributes[USER_KEY]

fun ApplicationCall.userOrNull(): User? =
    this.attributes.getOrNull(USER_KEY)

fun ApplicationCall.userId(): String =
    this.attributes[USER_ID_KEY]

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
