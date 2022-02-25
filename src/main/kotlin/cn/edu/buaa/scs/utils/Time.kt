package cn.edu.buaa.scs.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

const val defaultDateTimeFormatter = "yyyy/MM/dd HH:mm:ss"

fun String.toTimestamp(formatString: String = defaultDateTimeFormatter): Long {
    val zonedDateTime =
        LocalDateTime.parse(this, DateTimeFormatter.ofPattern(formatString)).atZone(ZoneId.of("Asia/Shanghai"))
    return zonedDateTime.toInstant().toEpochMilli()
}

class InstantSerializer : StdSerializer<Instant>(Instant::class.java) {
    override fun serialize(value: Instant?, gen: JsonGenerator?, provider: SerializerProvider?) {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
            .withZone(ZoneOffset.ofHours(8))
        gen?.writeString(value?.let { formatter.format(it) })
    }
}