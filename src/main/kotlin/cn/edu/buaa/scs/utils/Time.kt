package cn.edu.buaa.scs.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


class InstantSerializer : StdSerializer<Instant>(Instant::class.java) {
    override fun serialize(value: Instant?, gen: JsonGenerator?, provider: SerializerProvider?) {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
            .withZone(ZoneOffset.ofHours(8))
        gen?.writeString(value?.let { formatter.format(it) })
    }
}