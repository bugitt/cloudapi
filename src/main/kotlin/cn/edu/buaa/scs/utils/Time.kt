package cn.edu.buaa.scs.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val defaultDateTimeFormatter = "yyyy/MM/dd HH:mm:ss"

val zone: ZoneId = ZoneId.of("Asia/Shanghai")

fun String.toTimestamp(formatString: String = defaultDateTimeFormatter): Long {
    val zonedDateTime =
        LocalDateTime.parse(this, DateTimeFormatter.ofPattern(formatString)).atZone(zone)
    return zonedDateTime.toInstant().toEpochMilli()
}

fun Long.formatDateTime(formatString: String = defaultDateTimeFormatter): String {
    val zonedDateTime = Instant.ofEpochMilli(this).atZone(zone)
    return zonedDateTime.format(DateTimeFormatter.ofPattern(formatString))
}

object TimeUtil {
    fun currentDateTime(): String =
        System.currentTimeMillis().formatDateTime()
}