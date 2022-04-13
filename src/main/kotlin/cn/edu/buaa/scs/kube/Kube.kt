package cn.edu.buaa.scs.kube

import cn.edu.buaa.scs.error.BusinessException
import com.fkorotkov.kubernetes.*
import com.fkorotkov.kubernetes.apps.spec
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.apps.DaemonSet
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.api.model.apps.StatefulSet
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec
import kotlin.reflect.KClass

object Kube {

    private const val BUZTIME_LABEL = "buztime.cloud.scs"
    private const val CREATOR_LABEL = "creator.cloud.scs"
    private const val ID_LABEL = "id.cloud.scs"

    data class ParsedResult(
        val workload: Workload,
        val service: Service? = null,
        val pvcs: List<PersistentVolumeClaim>? = null,
    )

    private fun makeController(
        type: KClass<out Workload>,
        pod: PodTemplateSpec,
        meta: ObjectMeta,
        podSelector: LabelSelector
    ): Workload =
        when (type) {
            DeploymentWorkload::class -> DeploymentWorkload(
                Deployment().apply {
                    metadata = meta
                    spec {
                        replicas = 1
                        selector = podSelector
                        template = pod
                    }
                }
            )

            StatefulSetWorkload::class -> StatefulSetWorkload(
                StatefulSet().apply {
                    metadata = meta
                    spec {
                        replicas = 1
                        selector = podSelector
                        template = pod
                    }
                }
            )

            DaemonSetWorkload::class -> DaemonSetWorkload(
                DaemonSet().apply {
                    metadata = meta
                    spec {
                        selector = podSelector
                        template = pod
                    }
                }
            )

            JobWorkload::class -> JobWorkload(
                KubeJob().apply {
                    metadata = meta
                    spec = JobSpec().apply {
                        selector = podSelector
                        template = pod
                    }
                }
            )

            else -> throw BusinessException("unsupported workload type")
        }

    private fun parseDeployOption(opt: DeployOption): ParsedResult {
        val getVolName = fun(type: VolumeType) = when (type) {
            is VolumeType.AUTO -> opt.name
            is VolumeType.EXISTED -> type.value
        }

        val baseMeta: Map<String, String> = mapOf(
            ID_LABEL to opt.id,
            CREATOR_LABEL to opt.creator,
            BUZTIME_LABEL to System.currentTimeMillis().toString(),
        )
        val baseLabels: Map<String, String> = HashMap(baseMeta)
        val baseAnnotations: Map<String, String> = HashMap(baseMeta)
        val podSelector = newLabelSelector {
            matchLabels = mapOf("scs-cloud-api-selector" to opt.name)
        }
        val objectMeta = newObjectMeta {
            namespace = opt.namespace
            name = opt.name
            annotations = baseAnnotations
            labels = baseLabels
        }

        // pod依赖的volumes
        val podVolumes = opt.containers.flatMap { containerOpt ->
            containerOpt.mounts.flatMap { (volType, mountPoints) ->
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
        }

        val containerList = opt.containers.map { containerOpt ->
            // container中的挂载点
            val containerVolumeMounts = containerOpt.mounts.flatMap { (volType, mountPoints) ->
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
            val containerPorts = containerOpt.ports.map { port ->
                newContainerPort {
                    containerPort = port.containerPort
                    name = port.name
                    protocol = port.type.value
                }
            }

            newContainer {
                image = containerOpt.image
                env = containerOpt.envs.map { (k, v) ->
                    EnvVar().apply { name = k; value = v }
                }
                command = containerOpt.command
                volumeMounts = containerVolumeMounts
                ports = containerPorts
            }
        }

        // build pod
        val pod = newPodTemplateSpec {
            metadata = objectMeta
            spec {
                containers = containerList
                volumes = podVolumes
            }
        }

        val controller = makeController(opt.workloadType, pod, objectMeta, podSelector)

        // 需要创建的pvc
        val pvcs = opt.containers
            .mapNotNull { it.mounts[VolumeType.AUTO] }
            .flatten()
            .map {
                newPersistentVolumeClaim {
                    metadata = objectMeta
                    spec {
                        accessModes = listOf("ReadWriteMany")
                        resources {
                            requests = mapOf("storage" to Quantity("1Gi"))
                        }
                    }
                }
            }

        // 如果ports为空, 则无需创建service
        if (opt.containers.flatMap { it.ports }.isEmpty()) {
            return ParsedResult(
                workload = controller,
                pvcs = pvcs,
            )
        }

        val service = newService {
            metadata = objectMeta
            spec {
                selector = podSelector.matchLabels
                ports = containerList.flatMap { it.ports }.map { containerPort ->
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
            workload = controller,
            pvcs = pvcs,
        )
    }

    suspend fun asyncDeploy(opt: DeployOption): DeployResult {
        val result = parseDeployOption(opt)
        // create the pvcs
        // deploy the workload

        return DeployResult(opt.id)
    }
}