package cn.edu.buaa.scs.utils

fun String.getFileExtension(): String =
    if (this.endsWith(".tar.gz")) {
        "tar.gz"
    } else {
        this.split(".").let {
            if (it.size > 1) it.last()
            else ""
        }
    }