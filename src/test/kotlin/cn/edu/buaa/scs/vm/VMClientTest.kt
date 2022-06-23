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
                vmClient.getAllVMs().getOrNull()?.forEach { println(it) }
                delay(100000L)
            }
        }
    }

    @Test
    fun testPowerOnSync() {
        withApplication(testEnv) {
            runBlocking {
                val result = vmClient.powerOnSync("420700fa-85cd-c8a8-1b95-de8c169613ed")
                result.getOrThrow()
            }
        }
    }

    @Test
    fun testPowerOffSync() {
        withApplication(testEnv) {
            runBlocking {
                val result = vmClient.powerOffSync("420700fa-85cd-c8a8-1b95-de8c169613ed")
                result.getOrThrow()
            }
        }
    }
}