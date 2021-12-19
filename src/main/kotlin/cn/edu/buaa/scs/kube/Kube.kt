package cn.edu.buaa.scs.kube

import com.fkorotkov.kubernetes.*
import com.fkorotkov.kubernetes.apps.spec
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.DaemonSet
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec
import java.util.*

object Kube {

    data class ParsedResult(
        val controller: Controller,
        val service: Service? = null,
        val pvcs: List<PersistentVolumeClaim>? = null,
    )

    private fun makeController(
        type: ControllerType,
        pod: PodTemplateSpec,
        meta: ObjectMeta,
        podSelector: LabelSelector
    ): Controller =
        when (type) {
            ControllerType.DEPLOYMENT -> DeploymentController(
                Deployment().apply {
                    metadata = meta
                    spec {
                        replicas = 1
                        selector = podSelector
                        template = pod
                    }
                }
            )

            ControllerType.STATEFUL -> StatefulSetController(
                StatefulSet().apply {
                    metadata = meta
                    spec {
                        replicas = 1
                        selector = podSelector
                        template = pod
                    }
                }
            )


            ControllerType.DAEMON -> DaemonSetController(
                DaemonSet().apply {
                    metadata = meta
                    spec {
                        selector = podSelector
                        template = pod
                    }
                }
            )

            ControllerType.JOB -> JobController(
                KubeJob().apply {
                    metadata = meta
                    spec = JobSpec().apply {
                        selector = podSelector
                        template = pod
                    }
                }
            )
        }

    fun parseDeployOption(opt: DeployOption): ParsedResult {
        val getVolName = fun(type: VolumeType) = when (type) {
            is VolumeType.AUTO -> opt.name
            is VolumeType.EXISTED -> type.value
        }
        val baseLabels: Map<String, String> = mapOf(
            "scs-clout-api-id" to UUID.randomUUID().toString()
        )
        val baseAnnotations: Map<String, String> = mapOf(
            "scs-clout-api-id" to UUID.randomUUID().toString()
        )
        val podSelector = newLabelSelector {
            matchLabels = mapOf("scs-cloud-api-selector" to opt.name)
        }
        // parse pod
        // pod依赖的volumes
        val podVolumes = opt.mounts.flatMap { (volType, mountPoints) ->
            val volName = getVolName(volType)
            List(mountPoints.size) { i ->
                newVolume {
                    name = "$volName-$i"
                    persistentVolumeClaim {
                        claimName = volName
                    }
                }
            }
        }
        // container中的挂载点
        val containerVolumeMounts = opt.mounts.flatMap { (volType, mountPoints) ->
            val volName = getVolName(volType)
            mountPoints.mapIndexed { i, mount ->
                newVolumeMount {
                    name = "$volName-$i"
                    mountPath = mount.path
                    subPath = mount.subPath
                    readOnly = mount.readOnly
                }
            }
        }
        // container 对外暴露的port
        val containerPorts = opt.ports.map { port ->
            newContainerPort {
                containerPort = port.containerPort
                name = port.name
                protocol = port.type.value
            }
        }
        // build container
        val container = newContainer {
            image = opt.image
            env = opt.envs.map { (k, v) ->
                EnvVar().apply { name = k; value = v }
            }
            command = opt.command
            volumeMounts = containerVolumeMounts
            ports = containerPorts
        }
        // build pod
        val pod = newPodTemplateSpec {
            metadata {
                name = opt.name
                namespace = opt.namespace
                annotations = opt.podAnnotations + baseAnnotations
                labels = opt.podLabels + baseLabels
            }
            spec {
                containers = listOf(container)
                volumes = podVolumes
            }
        }

        // build controller
        val controllerMeta = ObjectMeta().apply {
            name = opt.name
            namespace = opt.namespace
            annotations = opt.controllerAnnotations + baseAnnotations
            labels = opt.controllerLabels + baseLabels
        }

        val controller = makeController(opt.controllerType, pod, controllerMeta, podSelector)

        // 需要创建的pvc
        val pvcs = opt.mounts.filter { it.key is VolumeType.AUTO }.map {
            newPersistentVolumeClaim {
                metadata {
                    name = name
                }
                spec {
                    storageClassName = "nfs"
                    accessModes = listOf("ReadWriteMany")
                    resources {
                        requests = mapOf("storage" to Quantity("1Gi"))
                    }
                }
            }
        }

        // 如果ports为空, 则无需创建service
        if (opt.ports.isEmpty()) {
            return ParsedResult(
                controller = controller,
                pvcs = pvcs,
            )
        }

        val service = newService {
            metadata {
                name = opt.name
                namespace = opt.namespace
                annotations = opt.serviceAnnotations + baseAnnotations
                labels = opt.serviceLabels + baseLabels
            }
            spec {
                selector = podSelector.matchLabels
                ports = containerPorts.map { containerPort ->
                    newServicePort {
                        name = containerPort.name
                        targetPort = IntOrString(containerPort.containerPort)
                        protocol = containerPort.protocol
                        port = containerPort.containerPort
                    }
                }
                type = "NodePort"
            }
        }

        return ParsedResult(
            service = service,
            controller = controller,
            pvcs = pvcs,
        )
    }

    suspend fun syncDeploy(opt: DeployOption): Result<DeployResult> {
        TODO()
    }
}