package cn.edu.buaa.scs.utils

import cn.edu.buaa.scs.error.ExecCommandException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

suspend fun String.runCommand() = withContext(Dispatchers.IO) {
    try {
        val exitCode = Runtime.getRuntime().exec(this@runCommand).waitFor()
        if (exitCode != 0) {
            throw Exception("Failed to execute command: $this@runCommand, exit code: $exitCode")
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        throw ExecCommandException("Failed to execute command: $this@runCommand", e)
    }
}

fun formatHeaders(headers: Map<String, List<String>>): String {
    return headers.map { (k, v) -> "$k: ${v.joinToString(" ")}" }.joinToString { "\n" }
}
