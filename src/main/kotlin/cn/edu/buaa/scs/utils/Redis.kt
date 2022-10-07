package cn.edu.buaa.scs.utils

import io.lettuce.core.RedisClient
import io.lettuce.core.SetArgs

/**
 * 连接 authRedis， 以判断token是否有效
 */
fun RedisClient.checkToken(token: String): String? =
    this.connect().use {
        val value = it.async().get(token)?.get()
        it.async().set(token, value, SetArgs.Builder.ex(60 * 60 * 24))
        value?.trim('"')?.lowercase()
    }
