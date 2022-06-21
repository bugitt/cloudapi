package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.testEnv
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class VMClientTest {
    @Test
    fun testGetAllVMs() {
        withApplication(testEnv) {
            runBlocking {
                vmClient.getAllVMs().forEach { println(it.toString()) }
                delay(100000L)
            }
        }
    }
}