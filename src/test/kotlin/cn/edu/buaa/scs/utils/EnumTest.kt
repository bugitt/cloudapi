package cn.edu.buaa.scs.utils

import cn.edu.buaa.scs.model.FileType
import org.junit.jupiter.api.Test

class EnumTest {
    @Test
    fun fileTypeFromString() {
        val a = FileType.valueOf("aa")
        println(a)
    }
}