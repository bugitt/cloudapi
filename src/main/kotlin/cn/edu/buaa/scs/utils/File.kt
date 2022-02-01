package cn.edu.buaa.scs.utils

fun String.getFileExtension(): String =
    this.split(".").let {
        if (it.size > 1) it.last()
        else ""
    }

fun String.updateFileExtension(newFilename: String?): String {
    val newExt = newFilename?.getFileExtension() ?: return this
    return if (newExt.isNotBlank()) {
        this.split(".").toMutableList().also { seq ->
            seq[seq.size - 1] = newExt
        }.joinToString(".")
    } else {
        this
    }
}