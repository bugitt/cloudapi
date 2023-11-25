package cn.edu.buaa.scs.utils

class Cache<K, V>(
    val fetch: (K) -> V,
    private var data: MutableMap<K, V> = mutableMapOf<K, V>()
) {
    fun get(k: K): V {
        if (data.containsKey(k)) {
            return data[k]!!
        }
        val v = this.fetch(k)
        data[k] = v
        return v
    }
}

fun <K, V, U> fetchInCache(fetch: (K) -> V, func: (Cache<K, V>) -> U): U {
    return func(Cache(fetch))
}
