package cn.edu.buaa.scs.kube

import cn.edu.buaa.scs.model.ContainerService
import cn.edu.buaa.scs.utils.schedule.waitForDone
import com.fkorotkov.kubernetes.*
import com.fkorotkov.kubernetes.apps.newDeployment
import com.fkorotkov.kubernetes.apps.spec
import com.fkorotkov.kubernetes.batch.v1.newJob
import com.fkorotkov.kubernetes.batch.v1.newJobSpec
import io.fabric8.kubernetes.api.model.IntOrString
import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.api.model.PodTemplateSpec
import io.fabric8.kubernetes.client.KubernetesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

open class KubeResourceCreationOptionBaseMeta(
    val name: String,
    val namespace: String,
    private val labels: Map<String, String> = mapOf(),
    private val annotations: Map<String, String> = mapOf(),
    val timeout: Long = 1000 * 60 * 60,
) {
    fun getObjectMeta(): ObjectMeta {
        val opt = this
        return newObjectMeta {
            this.name = opt.name
            this.namespace = opt.namespace
            this.labels = opt.labels
            this.annotations = opt.annotations
        }
    }
}

open class KubeResourceCreationOptionBaseMetaWithSelector(
    name: String,
    namespace: String,
    labels: Map<String, String> = mapOf(),
    annotations: Map<String, String> = mapOf(),
    val selectorLabels: Map<String, String> = mapOf(),
    timeout: Long = 1000 * 60 * 60,
) : KubeResourceCreationOptionBaseMeta(
    name,
    namespace,
    labels,
    annotations,
    timeout,
)

class PodControllerCreationOption(
    name: String,
    namespace: String,
    val podTemplateSpec: PodTemplateSpec,
    labels: Map<String, String> = mapOf(),
    annotations: Map<String, String> = mapOf(),
    selectorLabels: Map<String, String> = mapOf(),
    timeout: Long = 1000 * 60 * 60,
) : KubeResourceCreationOptionBaseMetaWithSelector(
    name,
    namespace,
    labels,
    annotations,
    selectorLabels,
    timeout,
)

suspend fun KubernetesClient.createJobSync(
    opt: PodControllerCreationOption,
    backoffLimit: Int = 4,
): Result<Unit> = runCatching {
    val job = newJob {
        metadata = opt.getObjectMeta()
        spec = newJobSpec {
            this.template = opt.podTemplateSpec
            this.backoffLimit = backoffLimit
        }
    }
    this.batch().v1().jobs().inNamespace(opt.namespace).resource(job).createOrReplace()
    waitForDone(opt.timeout) {
        val gotJob = this.batch().v1().jobs().inNamespace(opt.namespace).withName(opt.name).get()
        (gotJob.status?.succeeded ?: 0) > 0
    }.getOrThrow()
}

suspend fun KubernetesClient.createDeploymentSync(
    opt: PodControllerCreationOption,
    replicas: Int = 1,
): Result<Unit> = runCatching {
    val client = this
    withContext(Dispatchers.IO) {
        val deployment = newDeployment {
            metadata = opt.getObjectMeta()
            spec {
                this.template = opt.podTemplateSpec
                this.selector = newLabelSelector {
                    this.matchLabels = opt.selectorLabels
                }
                this.replicas = replicas
            }
        }
        client.apps().deployments().inNamespace(opt.namespace).resource(deployment).createOrReplace()
        client.apps().deployments().inNamespace(opt.namespace).withName(opt.name)
            .waitUntilReady(opt.timeout, TimeUnit.MILLISECONDS)
    }
}

suspend fun KubernetesClient.createServiceSync(
    opt: KubeResourceCreationOptionBaseMetaWithSelector,
    containerPorts: List<ContainerService.Port>,
    export: Boolean = false,
): Result<Unit> = runCatching {
    val client = this
    withContext(Dispatchers.IO) {
        val service = newService {
            metadata = opt.getObjectMeta()
            spec = newServiceSpec {
                this.selector = opt.selectorLabels
                this.type = if (export) "NodePort" else "ClusterIP"
                this.ports = containerPorts.map { srcPort ->
                    newServicePort {
                        this.name = srcPort.name
                        this.port = srcPort.port
                        this.targetPort = IntOrString(srcPort.port)
                        this.protocol = srcPort.protocol.toString()
                    }
                }
            }
        }
        client.services().inNamespace(opt.namespace).resource(service).createOrReplace()
        client.services().inNamespace(opt.namespace).withName(opt.name)
            .waitUntilReady(opt.timeout, TimeUnit.MILLISECONDS)
    }
}

suspend fun KubernetesClient.createConfigMapSync(
    opt: KubeResourceCreationOptionBaseMeta,
    data: Map<String, String>,
): Result<Unit> = runCatching {
    val client = this
    val configMap = newConfigMap {
        metadata = opt.getObjectMeta()
        this.data = data
    }
    withContext(Dispatchers.IO) {
        client.configMaps().inNamespace(opt.namespace).resource(configMap).createOrReplace()
        client.configMaps().inNamespace(opt.namespace).withName(opt.name)
            .waitUntilReady(opt.timeout, TimeUnit.MILLISECONDS)
    }
}