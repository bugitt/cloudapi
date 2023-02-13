package cn.edu.buaa.scs.utils

import io.ktor.http.*

val ContentType.value
    get() = "$contentType/$contentSubtype"