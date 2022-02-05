package cn.edu.buaa.scs.utils

import org.junit.jupiter.api.Test
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.*

internal class FileKtTest {

    @Test
    fun getFileExtension() {
        assert("example.docx".getFileExtension() == "docx")
        assert("ex".getFileExtension() == "")
        assert("".getFileExtension() == "")
    }

    @Test
    fun writeToNewFile() {
        val file = java.io.File(UUID.randomUUID().toString())
        FileOutputStream(file).use {
            it.write("123456".toByteArray())
        }
        assert(String(FileInputStream(file).readBytes()) == "123456")
        Files.delete(file.toPath())
    }
}