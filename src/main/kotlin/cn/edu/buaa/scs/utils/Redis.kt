package cn.edu.buaa.scs.utils

import io.lettuce.core.RedisClient

/**
 * 连接 authRedis， 以判断token是否有效
 */
fun RedisClient.checkToken(token: String): String? =
    this.connect().use {
        it.async().get(token)?.get()?.trim('"')?.lowercase()
    }
