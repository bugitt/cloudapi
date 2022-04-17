package cn.edu.buaa.scs.kube

import cn.edu.buaa.scs.utils.logger
import com.fkorotkov.kubernetes.newObjectMeta
import com.fkorotkov.kubernetes.newPersistentVolumeClaim
import com.fkorotkov.kubernetes.resources
import com.fkorotkov.kubernetes.spec
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.WatcherException
import io.ktor.application.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test


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
    fun testWatch() {
        withTestApplication(Application::kubeModule) {
            val client = kubeClient()

            client.persistentVolumeClaims().watch(object : Watcher<PersistentVolumeClaim> {
                override fun eventReceived(action: Watcher.Action?, resource: PersistentVolumeClaim?) {
                    logger()().info("Watch event received {}: {}", action?.name, resource?.metadata?.name)
                }

                override fun onClose(cause: WatcherException?) {
                    logger()().info("Watch gracefully closed")
                }

            }).use {
                val pvc = client.persistentVolumeClaims().create(
                    newPersistentVolumeClaim {
                        metadata = newObjectMeta {
                            name = "test"
                            namespace = "default"
                        }
                        spec {
                            accessModes = listOf("ReadWriteMany")
                            resources {
                                requests = mapOf("storage" to Quantity("1Gi"))
                            }
                        }
                    }
                )
                pvc?.let {
                    client.persistentVolumeClaims().delete(it)
                }
            }
        }
    }
}