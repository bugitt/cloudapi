package cn.edu.buaa.scs.utils

import io.ktor.server.application.*
import java.io.InputStream

fun Application.getConfigString(name: String, default: String = ""): String =
    this.environment.config.propertyOrNull(name)?.getString() ?: default

fun Application.getConfigList(name: String, default: List<String> = listOf()): List<String> =
    this.environment.config.propertyOrNull(name)?.getList() ?: default

fun Application.getFile(filename: String): InputStream {
    return this.javaClass.getResourceAsStream(filename)!!
}