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

    @Test
    fun testCreateVm() {
        withApplication(testEnv) {
            runBlocking {
                val result = vmClient.createVM(
                    CreateVmOptions(
                        name = "loheagn-test",
                        templateUuid = "4207e974-8edd-8555-abdc-a664fabf92a3",
                        applyId = "test-apply-id",
                        memory = 8192,
                        cpu = 8,
                        diskSize = 16106127360,
                    )
                )
                result.getOrThrow()
            }
        }
    }

    @Test
    fun testDeleteVm() {
        withApplication(testEnv) {
            runBlocking {
                val result = vmClient.deleteVM("4207002c-3175-941b-f1ae-3c91af32c627")
                result.getOrThrow()
            }
        }
    }
}