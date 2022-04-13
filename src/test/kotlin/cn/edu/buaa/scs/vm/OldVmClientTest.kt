package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.testEnv
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class OldVmClientTest {
    @Test
    fun testGetVmInfo() {
        withApplication(testEnv) {
            runBlocking {
                println(OldVmClient.getVmInfo("debian11-ks-2"))
            }
        }
    }
}