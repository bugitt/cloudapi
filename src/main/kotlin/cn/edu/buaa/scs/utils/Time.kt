package cn.edu.buaa.scs.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.time.LocalDateTime
import java.time.ZoneOffset


fun LocalDateTime.format(): Long =
    this.toInstant(ZoneOffset.ofHours(8)).toEpochMilli()


class LocalDateTimeSerializer : StdSerializer<LocalDateTime>(LocalDateTime::class.java) {
    override fun serialize(value: LocalDateTime?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeNumber(value?.format() ?: 0)
    }
}