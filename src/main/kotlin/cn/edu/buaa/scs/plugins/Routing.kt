package cn.edu.buaa.scs.plugins

import cn.edu.buaa.scs.auth.authRoute
import cn.edu.buaa.scs.authRedis
import cn.edu.buaa.scs.error.AuthenticationException
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.extensions.checkToken
import cn.edu.buaa.scs.extensions.info
import cn.edu.buaa.scs.model.Users
import cn.edu.buaa.scs.utils.TOKEN_KEY
import cn.edu.buaa.scs.utils.USER_KEY
import cn.edu.buaa.scs.utils.test.test
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.routing.*

fun Application.configureRouting() {

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

        // just for test
        call.info(call.request.path())
        if (call.request.path() == "/test") {
            return
        }

        // TODO 添加其他不需要token例外情况
        if (token.isEmpty()) {
            throw AuthenticationException()
        }

        // 校验token
        val user = authRedis.checkToken(token)?.let { Users.getByID(it) }
            ?: throw throw AuthorizationException("incorrect token")


        call.attributes.put(TOKEN_KEY, token)
        call.attributes.put(USER_KEY, user)
    }

    intercept(ApplicationCallPipeline.Call) {
        fetchToken(call)
    }

    routing {
        route("/api/v2") {
            authRoute()
            // 添加其他的 route
        }

        // just for test
        test()
    }
}
