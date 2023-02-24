package cn.edu.buaa.scs.utils

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.ktorm.jackson.KtormModule

val jsonMapper = jacksonObjectMapper().also {
    it.registerModule(KtormModule())
    it.registerModule(Jdk8Module())
}

inline fun <reified T> jsonReadValue(str: String): T = jsonMapper.readValue(str)

fun convertToMap(obj: Any): Map<String, String> {
    return jsonReadValue(jsonMapper.writeValueAsString(obj))
}
