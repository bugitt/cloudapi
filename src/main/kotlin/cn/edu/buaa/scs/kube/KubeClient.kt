package cn.edu.buaa.scs.kube

import cn.edu.buaa.scs.utils.getFile
import cn.edu.buaa.scs.utils.logger
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.ktor.application.*

lateinit var kubeClient: () -> KubernetesClient

@Suppress("unused")
fun Application.kubeModule() {
    val configString = getFile("/kubeconfig.yaml").readText()
    kubeClient = fun(): KubernetesClient {
        return DefaultKubernetesClient.fromConfig(configString)
    }
    KubeOpScheduler.launch()
    kubeClient().use {
        logger("kube")().info { "connected to kubernetes-appserver successfully: ${it.kubernetesVersion.gitVersion}" }
    }
}