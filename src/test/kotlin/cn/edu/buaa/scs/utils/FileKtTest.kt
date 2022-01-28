package cn.edu.buaa.scs.utils

import org.junit.jupiter.api.Test

internal class FileKtTest {

    @Test
    fun getFileExtension() {
        assert("example.docx".getFileExtension() == "docx")
        assert("ex".getFileExtension() == "")
        assert("".getFileExtension() == "")
    }
}