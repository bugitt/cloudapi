package cn.edu.buaa.scs.kube

import io.fabric8.kubernetes.client.KubernetesClient
import io.ktor.server.application.*

lateinit var kubeClient: () -> KubernetesClient

@Suppress("unused")
fun Application.kubeModule() {
//    val configString = String(getFile("/kubeconfig.yaml").readAllBytes())
//    kubeClient = fun(): KubernetesClient {
//        return DefaultKubernetesClient.fromConfig(configString)
//    }
//    KubeOpScheduler.launch()
//    kubeClient().use {
//        logger("kube")().info { "connected to kubernetes-appserver successfully: ${it.kubernetesVersion.gitVersion}" }
//    }
}