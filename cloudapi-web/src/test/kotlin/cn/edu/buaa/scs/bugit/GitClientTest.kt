package cn.edu.buaa.scs.bugit

import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class GitClientTest {
    @Test
    fun getCurrentUser() = testApplication {
        application {
            runBlocking {
                val user = GitClient.get<GitUser>("user")
                assert(user.isSuccess)
                println(user.getOrThrow())
            }
        }
    }

    @Test
    fun deleteProject() = testApplication {
        application {
            runBlocking {
                GitClient.deleteProject("admin-org").getOrThrow()
                // call multiple times
                GitClient.deleteProject("admin-org").getOrThrow()
            }
        }
    }
}