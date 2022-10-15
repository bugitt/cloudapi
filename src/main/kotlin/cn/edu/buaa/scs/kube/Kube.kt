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


internal const val defaultTimeout = 1000 * 60 * 60L

open class NameWithNamespace(
    val name: String,
    val namespace: String
)

open class KubeResourceCreationOptionBaseMeta(
    name: String,
    namespace: String,
    private val labels: Map<String, String> = mapOf(),
    private val annotations: Map<String, String> = mapOf(),
    val timeout: Long = defaultTimeout,
) : NameWithNamespace(name, namespace) {
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
    timeout: Long = defaultTimeout,
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
    timeout: Long = defaultTimeout,
    val rerun: Boolean = false,
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
    restartPolicy: String = "Never",
    backoffLimit: Int = 4,
): Result<Unit> = runCatching {
    val job = newJob {
        metadata = opt.getObjectMeta()
        spec = newJobSpec {
            this.template = opt.podTemplateSpec.also { it.spec.restartPolicy = restartPolicy }
            this.backoffLimit = backoffLimit
        }
    }
    if (opt.rerun) {
        this.deleteJob(opt).getOrThrow()
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
        if (opt.rerun) {
            if (client.restartDeployment(opt).getOrThrow()) {
                return@withContext
            }
        }
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

suspend fun KubernetesClient.restartDeployment(
    nameWithNamespace: NameWithNamespace,
    timeout: Long = (nameWithNamespace as? PodControllerCreationOption)?.timeout ?: defaultTimeout,
): Result<Boolean> = runCatching {
    val client = this
    withContext(Dispatchers.IO) {
        val deploymentOperation = client.apps().deployments()
            .inNamespace(nameWithNamespace.namespace)
            .withName(nameWithNamespace.name)
        if (deploymentOperation.get() == null) return@withContext false
        deploymentOperation.rolling().restart()
        client.apps().deployments().inNamespace(nameWithNamespace.namespace).withName(nameWithNamespace.name)
            .waitUntilReady(timeout, TimeUnit.MILLISECONDS)
        true
    }
}

suspend fun KubernetesClient.deleteJob(
    nameWithNamespace: NameWithNamespace,
    timeout: Long = (nameWithNamespace as? PodControllerCreationOption)?.timeout ?: defaultTimeout,
): Result<Unit> = runCatching {
    val client = this
    withContext(Dispatchers.IO) {
        val jobOperation = client.batch().v1().jobs()
            .inNamespace(nameWithNamespace.namespace)
            .withName(nameWithNamespace.name)
        if (jobOperation.get() == null) return@withContext
        jobOperation.delete()
        waitForDone(timeout) {
            client.batch().v1().jobs().inNamespace(nameWithNamespace.namespace).withName(nameWithNamespace.name)
                .get() == null
        }.getOrThrow()
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