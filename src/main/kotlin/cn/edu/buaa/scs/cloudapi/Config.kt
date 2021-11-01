package cn.edu.buaa.scs.cloudapi

import cn.edu.buaa.scs.cloudapi.error.PropertyNotFoundException
import org.springframework.core.env.Environment
import redis.clients.jedis.JedisPool
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class EnvInitialHelper : ReadOnlyProperty<Any, Environment> {
    companion object {
        var env: Environment? = null
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): Environment {
        return env!!
    }
}

object Config {
    val env by EnvInitialHelper()
    inline fun <reified T : Any> getEnvVal(key: String): T {
        return env.getProperty(key, T::class.java) ?: throw PropertyNotFoundException(key)
    }

    fun getString(key: String) = getEnvVal<String>(key)
    fun getInt(key: String) = getEnvVal<Int>(key)
}

open class Redis(val initializer: () -> JedisPool) {
    private val pool: JedisPool by lazy { initializer() }
    fun getString(key: String): String? {
        val jedis = pool.resource
        val result = jedis.get(key)
        pool.returnResource(jedis)
        return result
    }
}

