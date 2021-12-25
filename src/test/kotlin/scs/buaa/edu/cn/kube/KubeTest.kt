package scs.buaa.edu.cn.kube

import cn.edu.buaa.scs.kubeClient
import cn.edu.buaa.scs.kubeModule
import io.ktor.application.*
import io.ktor.server.testing.*
import org.junit.Test

class KubeTest {
    @Test
    fun testKubernetesInit() {
        withTestApplication(Application::kubeModule) {
            val client = kubeClient()
            println(client.kubernetesVersion.gitVersion)
            assert(client.kubernetesVersion.gitVersion.isNotBlank())
        }
    }
}