package cn.edu.buaa.scs.kube

import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.WatcherException
import io.ktor.server.application.*
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

    @Test
    fun testKubernetesEventWatch() {
        withTestApplication(Application::kubeModule) {
            kubeClient().use { client ->
                client.batch().v1().jobs()
                client.apps().deployments().inAnyNamespace().watch(object : Watcher<Deployment> {
                    override fun eventReceived(action: Watcher.Action?, resource: Deployment?) {
                        println("$action, $resource")
                    }

                    override fun onClose(cause: WatcherException?) {
                        println(cause.toString())
                    }

                }).use {
                    Thread.sleep(100000L)
                }
            }

        }
    }
}