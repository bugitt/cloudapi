package cn.edu.buaa.scs.auth

import cn.edu.buaa.scs.cache.authRedis
import cn.edu.buaa.scs.error.AuthenticationException
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.model.User
import cn.edu.buaa.scs.model.UserRole
import cn.edu.buaa.scs.service.id
import cn.edu.buaa.scs.utils.*
import cn.edu.buaa.scs.utils.encrypt.RSAEncrypt
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*

lateinit var superTokenList: List<String>

val adminUser = User {
    id = "admin"
    name = "admin"
    email = "admin@buaa.edu.cn"
    role = UserRole.SYS
}

data class TokenInfo(
    @JsonProperty("user_id") val userId: String,
    @JsonProperty("created_time") val createdTime: Long,
)

// 如有需要，可以继续添加其他不需要鉴权的例外情况
val escapeApiMap = mapOf(
    "/api/v2/captcha" to listOf(HttpMethod.Get),
    "/api/v2/login" to listOf(HttpMethod.Post),
    "/api/v2/buaaSSOLogin" to listOf(HttpMethod.Post),
    "/test" to listOf(HttpMethod.Get),
)

fun generateRSAToken(userId: String): String {
    val tokenInfo = TokenInfo(userId, System.currentTimeMillis())
    return RSAEncrypt.encrypt(jsonMapper.writeValueAsString(tokenInfo))
}

internal val possibleTokenKey =
    listOf("scs-token", "token", "Token", "authorization", "Authorization", "authentication", "Authentication")

/**
 * 尝试各种方法fetch token
 * 并给出身份识别
 * 兼容旧平台
 */
fun fetchToken(call: ApplicationCall) {
    val token: String = when {
        call.isWS() -> {
            call.request.path().split("/").last()
        }

        else -> {
            // TODO 后续兼容JWT校验

            // 1. try to get token from cookies
            possibleTokenKey.firstNotNullOfOrNull { call.request.cookies[it] } ?:

            // 2. try to get token from headers
            possibleTokenKey.firstNotNullOfOrNull {
                call.request.headers[it]?.let { auth ->
                    if (auth.startsWith("Bearer")) auth.split(" ")[1] else auth
                }
            } ?:

            // 3. try to get token from query parameters
            possibleTokenKey.firstNotNullOfOrNull { call.request.queryParameters[it] }

            ?: ""
        }
    }

    val setUser = fun(user: User) {
        call.attributes.put(TOKEN_KEY, token)
        call.attributes.put(USER_KEY, user)
        call.attributes.put(USER_ID_KEY, user.id)
    }

    if (token.isEmpty()) {
        val canEscape = call.request.httpMethod == HttpMethod.Options ||
                (escapeApiMap[call.request.path()]?.contains(call.request.httpMethod) ?: false)
        if (canEscape) {
            return
        }
        throw AuthenticationException()
    }

    // super token
    if (superTokenList.contains(token)) {
        val user = call.request.headers["X-Custom-User"]?.let { User.id(it) } ?: adminUser
        setUser(user)
        return
    }

    val userId =
        // rsa token
        RSAEncrypt.decrypt(token).getOrNull()?.let { tokenInfo ->
            jsonReadValue<TokenInfo>(tokenInfo).userId
        } ?:
        // redis uuid token
        authRedis.checkToken(token) ?:
        // error
        throw throw AuthorizationException("incorrect token")

    val user = User.id(userId)
    setUser(user)
}

fun ApplicationCall.isWS(): Boolean {
    return request.path().startsWith("/api/v2/ws")
}

@Suppress("unused")
fun Application.authModule() {
    superTokenList = getConfigList("auth.superTokenList")
}
