package cn.edu.buaa.scs.kube

import cn.edu.buaa.scs.utils.getConfigString
import cn.edu.buaa.scs.utils.logger
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.ktor.server.application.*

lateinit var kubeClient: () -> KubernetesClient

@Suppress("unused")
fun Application.kubeModule() {
    val masterUrl = getConfigString("kube.business.masterUrl")
    val token = getConfigString("kube.business.token")
    kubeClient = fun(): KubernetesClient {
        val config = ConfigBuilder()
            .withMasterUrl(masterUrl)
            .withApiVersion("v1")
            .withOauthToken(token)
            .build()
        return KubernetesClientBuilder().withConfig(config).build()
    }
    KubeOpScheduler.launch()
    kubeClient().use {
        logger("kube")().info { "connected to business-kubernetes-appserver successfully: ${it.kubernetesVersion.gitVersion}" }
    }
}