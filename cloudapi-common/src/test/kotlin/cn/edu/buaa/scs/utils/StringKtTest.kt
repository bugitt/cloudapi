package cn.edu.buaa.scs.utils

import io.fabric8.kubernetes.client.KubernetesClientBuilder
import org.junit.jupiter.api.Test

class StringKtTest {

    @Test
    fun ensureNamespace() {
        val kubeClient = KubernetesClientBuilder().build()
        val nsName = "some-ns"
        nsName.ensureNamespace(kubeClient)
        assert(kubeClient.namespaces().withName(nsName).get() != null)
        kubeClient.namespaces().withName(nsName).delete()
    }
}
