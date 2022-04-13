package cn.edu.buaa.scs.kube

data class BuildOption(
    // TODO
    val name: String,
)

data class DeployOption(
    val name: String,
    val namespace: String,
    val image: String,

    // pod options
    val podLabels: Map<String, String> = mapOf(),
    val podAnnotations: Map<String, String> = mapOf(),
    val envs: Map<String, String> = mapOf(),
    val command: List<String> = listOf(),
    val mounts: Map<VolumeType, List<MountPoint>> = mapOf(),

    // controller options
    val needInternet: Boolean = false,
    val workloadType: WorkloadType = WorkloadType.DEPLOYMENT,
    val controllerLabels: Map<String, String> = mapOf(),
    val controllerAnnotations: Map<String, String> = mapOf(),

    // export ports
    val ports: List<Port> = listOf(),
    val serviceLabels: Map<String, String> = mapOf(),
    val serviceAnnotations: Map<String, String> = mapOf(),
)

typealias KubeJob = io.fabric8.kubernetes.api.model.batch.v1.Job

data class DeployResult(
    val uuid: String,
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
