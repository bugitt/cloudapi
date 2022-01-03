package cn.edu.buaa.scs.cache

import cn.edu.buaa.scs.getConfigString
import cn.edu.buaa.scs.utils.logger
import io.ktor.application.*
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI

lateinit var authRedis: RedisClient

@Suppress("unused")
fun Application.redisModule() {
    // redis for auth
    authRedis = RedisClient.create(
        RedisURI.Builder.redis(getConfigString("redis.auth.host", "localhost"))
            .withPort(getConfigString("redis.auth.port", "6379").toInt())
            .withPassword(getConfigString("redis.auth.password", "").toCharArray())
            .build()
    )
    logger("auth-redis")().info { "auth redis connected" }
}