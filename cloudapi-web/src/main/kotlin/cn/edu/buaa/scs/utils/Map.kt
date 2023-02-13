package cn.edu.buaa.scs.utils

fun <K, V> MutableMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    return this[key] ?: defaultValue().also { this[key] = it }
}