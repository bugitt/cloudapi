package cn.edu.buaa.scs.kube.crd.v1alpha1

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList
import io.fabric8.kubernetes.api.model.KubernetesResource
import io.javaoperatorsdk.operator.api.reconciler.*
import cn.edu.buaa.scs.model.VirtualMachine as VirtualMachineModel

class VirtualMachineList() : DefaultKubernetesResourceList<VirtualMachine>()

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VirtualMachineSpec(
    @JsonProperty("uuid") val uuid: String,
    @JsonProperty("platform") val platform: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("isTemplate") val isTemplate: Boolean,
    @JsonProperty("host") val host: String,

    @JsonProperty("adminId") val adminId: String,
    @JsonProperty("studentId") val studentId: String,
    @JsonProperty("teacherId") val teacherId: String,
    @JsonProperty("isExperimental") val isExperimental: Boolean,
    @JsonProperty("experimentId") val experimentId: Int,
    @JsonProperty("applyId") val applyId: String,

    @JsonProperty("cpu") val cpu: Int,
    @JsonProperty("memory") val memory: Int, //MB
    @JsonProperty("diskNum") val diskNum: Int,
    @JsonProperty("diskSize") val diskSize: Int, // bytes
    @JsonProperty("osFullName") val osFullName: String,
    @JsonProperty("powerState") val powerState: VirtualMachineModel.PowerState,

    ) : KubernetesResource

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VirtualMachineStatus(
    @JsonProperty("locking") val locking: Boolean,
    @JsonProperty("overallStatus") val overallStatus: VirtualMachineModel.OverallStatus,
    @JsonProperty("netInfos") val netInfos: List<VirtualMachineModel.NetInfo>,
)

@ControllerConfiguration(
    generationAwareEventProcessing = false,
)
class VirtualMachineReconciler : Reconciler<VirtualMachine>, Cleaner<VirtualMachine> {
    override fun reconcile(
        resource: VirtualMachine?,
        context: Context<VirtualMachine>?
    ): UpdateControl<VirtualMachine> {
        TODO()
    }

    override fun cleanup(resource: VirtualMachine?, context: Context<VirtualMachine>?): DeleteControl {
        TODO()
    }

}
