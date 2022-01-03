package cn.edu.buaa.scs.auth

import cn.edu.buaa.scs.authRedis
import cn.edu.buaa.scs.error.AuthenticationException
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.extensions.checkToken
import cn.edu.buaa.scs.model.User
import cn.edu.buaa.scs.model.Users
import cn.edu.buaa.scs.utils.TOKEN_KEY
import cn.edu.buaa.scs.utils.USER_ID_KEY
import cn.edu.buaa.scs.utils.USER_KEY
import io.ktor.application.*
import io.ktor.request.*

lateinit var superTokenList: List<String>

val adminUser = User {
    id = "admin"
    name = "admin"
    email = "admin@buaa.edu.cn"
}

/**
 * 尝试各种方法fetch token
 * 并给出身份识别
 * 目前仅兼容旧平台的用法
 */
fun fetchToken(call: ApplicationCall) {
    // TODO 后续兼容JWT校验
    val token: String = call.request.queryParameters.let { params ->
        params["authentication"] ?: params["Authentication"]
    } ?: call.request.headers.let { headers ->
        headers["authorization"] ?: headers["Authorization"]
    }?.let { auth -> auth.split(" ").let { if (it.size > 1 && it[0] == "Bearer") it[1] else auth } } ?: ""

    // TODO 添加其他不需要token例外情况
    if (token.isEmpty()) {
        throw AuthenticationException()
    }

    val setUser = fun(user: User) {
        call.attributes.put(TOKEN_KEY, token)
        call.attributes.put(USER_KEY, user)
        call.attributes.put(USER_ID_KEY, user.id)
    }

    // just for test
    if (call.request.path() == "/test") {
        return
    }

    // super token
    if (superTokenList.contains(token)) {
        setUser(adminUser)
        return
    }

    // common token
    val user = authRedis.checkToken(token)?.let { Users.getByID(it) }
        ?: throw throw AuthorizationException("incorrect token")
    setUser(user)
}