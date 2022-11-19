package cn.edu.buaa.scs.utils

// 项目名称需符合 RFC 1035 中的 DNS 标签规范
fun String.isValidProjectName(): Boolean {
    return this.length <= 23 &&
            this.all { it.isLowerCase() || it.isDigit() || it == '-' || it == '.' } &&
            this.first().isLowerCase() &&
            this.last().isLetterOrDigit()
}

fun String.tryToInt(): Int? {
    return try {
        this.toInt()
    } catch (e: NumberFormatException) {
        null
    }
}