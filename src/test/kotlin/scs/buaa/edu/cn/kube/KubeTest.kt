package scs.buaa.edu.cn.kube

import cn.edu.buaa.scs.kubeClient
import cn.edu.buaa.scs.kubeModule
import io.fabric8.kubernetes.api.model.events.v1.Event
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.WatcherException
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

    @Test
    fun testKubernetesEventWatch() {
        withTestApplication(Application::kubeModule) {
            kubeClient().use { client ->
                client.events().v1().events().inAnyNamespace().watch(object : Watcher<Event> {
                    override fun eventReceived(action: Watcher.Action, resource: Event) {
                        println("event: ${action.name}, resource: $resource")
                    }

                    override fun onClose(cause: WatcherException?) {
                        println("close cause: $cause")
                    }
                }).use {
                    Thread.sleep(100000L)
                }
            }

        }
    }
}