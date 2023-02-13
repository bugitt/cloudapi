package cn.edu.buaa.scs.utils

import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class IOTest {
    @Test
    fun inputSteamAppendToOutputStream() {
        val result = String(ByteArrayOutputStream().use { output ->
            "123".byteInputStream().use { it.copyTo(output) }
            "456".byteInputStream().use { it.copyTo(output) }
            output.toByteArray()
        })
        println(result)
        assert(result == "123456")
    }
}