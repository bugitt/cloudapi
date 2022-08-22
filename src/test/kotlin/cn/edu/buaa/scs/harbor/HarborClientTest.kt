package cn.edu.buaa.scs.harbor

import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class HarborClientTest {
    @Test
    fun createProjectForAdmin() = testApplication {
        application {
            runBlocking {
                HarborClient.createProjectForUser("admin", "admin-api-test", "", "").getOrThrow()
            }
        }
    }
}