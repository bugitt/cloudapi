package cn.edu.buaa.scs.extensions

import cn.edu.buaa.scs.model.User
import cn.edu.buaa.scs.utils.TOKEN_KEY
import cn.edu.buaa.scs.utils.USER_KEY
import cn.edu.buaa.scs.utils.logger
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.request.*

fun ApplicationCall.token(): String =
    this.attributes[TOKEN_KEY]

fun ApplicationCall.user(): User =
    this.attributes[USER_KEY]

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