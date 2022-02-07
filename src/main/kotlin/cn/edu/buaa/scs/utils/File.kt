package cn.edu.buaa.scs.utils

fun String.getFileExtension(): String {
    val specialList = listOf(
        "docx",
        "doc",
        "zip",
        "pdf",
        "tar.gz"
    )
    for (name in specialList) {
        if (this.endsWith(".$name")) {
            return name
        }
    }
    return this.split(".").let {
        if (it.size > 1) it.last()
        else ""
    }
}