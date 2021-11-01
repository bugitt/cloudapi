package cn.edu.buaa.scs.cloudapi.service

import cn.edu.buaa.scs.cloudapi.Config
import cn.edu.buaa.scs.cloudapi.Redis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

object AuthRedis : Redis({
    JedisPool(
        JedisPoolConfig(),
        Config.getString("redis.host"),
        Config.getInt("redis.port"),
        Config.getInt("redis.timeout"),
        Config.getString("redis.password")
    )
}) {
    fun getId(token: String) = getString(token)?.trim('"')?.lowercase()
}