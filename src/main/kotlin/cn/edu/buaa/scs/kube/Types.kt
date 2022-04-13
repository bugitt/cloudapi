package cn.edu.buaa.scs.kube

import kotlin.reflect.KClass

data class BuildOption(
    // TODO
    val name: String,
)

data class DeployOption(
    val name: String,
    val namespace: String,
    val id: String,
    val creator: String,

    // pod option
    val containers: List<ContainerOption> = listOf(),

    // controller options
    val needInternet: Boolean = false,
    val workloadType: KClass<out Workload> = DeploymentWorkload::class,
)

data class ContainerOption(
    val image: String,
    val isInitContainer: Boolean,
    val envs: Map<String, String> = mapOf(),
    val command: List<String> = listOf(),
    val mounts: Map<VolumeType, List<MountPoint>> = mapOf(),
    // export ports
    val ports: List<Port> = listOf(),
)

typealias KubeJob = io.fabric8.kubernetes.api.model.batch.v1.Job

data class DeployResult(
    val uuid: String
)

sealed class VolumeType {
    object AUTO : VolumeType()
    data class EXISTED(val value: String) : VolumeType()
}

data class MountPoint(
    val name: String,
    val readOnly: Boolean,
    val path: String,
    val subPath: String = "",
)

enum class NetProtocol(val value: String) {
    TCP("TCP"),
    UDP("UDP"),
}

data class Port(
    val containerPort: Int,
    val name: String = containerPort.toString(),
    val type: NetProtocol = NetProtocol.TCP,
)
