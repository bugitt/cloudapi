package cn.edu.buaa.scs.utils

import io.ktor.application.*
import java.io.File

fun Application.getConfigString(name: String, default: String = ""): String =
    this.environment.config.propertyOrNull(name)?.getString() ?: default

fun Application.getConfigList(name: String, default: List<String> = listOf()): List<String> =
    this.environment.config.propertyOrNull(name)?.getList() ?: default

fun Application.getFile(filename: String): File =
    File(this.javaClass.getResource(filename)?.toURI()!!)