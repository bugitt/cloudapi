package cn.edu.buaa.scs.utils

import io.lettuce.core.RedisClient

/**
 * 连接 authRedis， 以判断token是否有效
 */
fun RedisClient.checkToken(token: String): String? =
    this.connect().use {
        val value = it.async().get(token)?.get() ?: return null
        it.async().set(token, value)
        jsonReadValue<String>(value).lowercase()
    }

fun RedisClient.setToken(token: String, userId: String) =
    this.connect().use {
        it.async().set(token, "\"$userId\"")
        Unit
    }

fun RedisClient.setUsedStr(str: String) =
    this.connect().use {
        it.async().set(str, "true")
    }

fun RedisClient.checkStrUsed(str: String): Boolean =
    this.connect().use {
        it.async().get(str)?.get() != null
    }

fun RedisClient.getValueByKey(key: String): String? =
    this.connect().use {
        it.async().get(key)?.get()
    }

fun RedisClient.setExpireKey(key: String, value: String, expireTime: Long) =
    this.connect().use {
        it.async().setex(key, expireTime, value)
        Unit
    }
