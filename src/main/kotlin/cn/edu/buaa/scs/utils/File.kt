package cn.edu.buaa.scs.utils

fun String.getFileExtension(): String =
    this.split(".").let {
        if (it.size > 1) it.last()
        else ""
    }