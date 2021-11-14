package cn.edu.buaa.scs.extensions

import io.lettuce.core.RedisClient

/**
 * 连接 authRedis， 以判断token是否有效
 */
fun RedisClient.checkToken(token: String): String? =
    this.connect().async().get(token)?.get()?.trim('"')?.lowercase()
