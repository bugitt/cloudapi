package cn.edu.buaa.scs.kube

import cn.edu.buaa.scs.image.ImageBuildRoutine
import cn.edu.buaa.scs.utils.getConfigString
import cn.edu.buaa.scs.utils.logger
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.utils.Serialization
import io.ktor.server.application.*
import org.ktorm.jackson.KtormModule

lateinit var businessKubeClientBuilder: () -> KubernetesClient

val kubeClient: KubernetesClient by lazy { KubernetesClientBuilder().build() }

@Suppress("unused")
fun Application.kubeModule() {
    val masterUrl = getConfigString("kube.business.masterUrl")
    val token = getConfigString("kube.business.token")
    businessKubeClientBuilder = fun(): KubernetesClient {
        val config = ConfigBuilder()
            .withMasterUrl(masterUrl)
            .withApiVersion("v1")
            .withOauthToken(token)
            .build()
        return KubernetesClientBuilder().withConfig(config).build()
    }
    businessKubeClientBuilder().use {
        logger("business-kube")().info { "connected to business kubernetes apiserver successfully: ${it.kubernetesVersion.gitVersion}" }
    }

    Serialization.jsonMapper().registerModules(kotlinModule(), KtormModule())

    logger("kube")().info { "connected to official kubernetes apiserver successfully: ${kubeClient.kubernetesVersion.gitVersion}" }
    registerVirtualMachineOperator()

    ImageBuildRoutine.run()
}
