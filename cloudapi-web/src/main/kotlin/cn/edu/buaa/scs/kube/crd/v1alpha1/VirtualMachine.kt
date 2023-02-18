package cn.edu.buaa.scs.kube.crd.v1alpha1

import cn.edu.buaa.scs.vm.newVMClient
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList
import io.fabric8.kubernetes.api.model.KubernetesResource
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.model.annotation.PrinterColumn
import io.javaoperatorsdk.operator.api.reconciler.*
import kotlinx.coroutines.runBlocking
import cn.edu.buaa.scs.model.VirtualMachine as VirtualMachineModel

class VirtualMachineList() : DefaultKubernetesResourceList<VirtualMachine>()

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = JsonDeserializer.None::class)
data class VirtualMachineSpec(
    @JsonProperty("uuid") val uuid: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("platform") val platform: String,
    @JsonProperty("template") val template: Boolean?,
    @JsonProperty("host") val host: String?,

    @JsonProperty("adminId") val adminId: String?,
    @JsonProperty("studentId") val studentId: String?,
    @JsonProperty("teacherId") val teacherId: String?,
    @JsonProperty("experimental") val experimental: Boolean?,
    @JsonProperty("experimentId") val experimentId: Int?,
    @JsonProperty("applyId") val applyId: String?,

    @JsonProperty("cpu") val cpu: Int,
    @JsonProperty("memory") val memory: Int, //MB
    @JsonProperty("diskNum") val diskNum: Int,
    @JsonProperty("diskSize") val diskSize: Long, // bytes
    @JsonProperty("osFullName") val osFullName: String,
    @JsonProperty("powerState") @PrinterColumn(name = "spec_powerState") val powerState: VirtualMachineModel.PowerState?,

    @JsonProperty("deleted") val deleted: Boolean? = false,
) : KubernetesResource

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = JsonDeserializer.None::class)
data class VirtualMachineStatus(
    @JsonProperty("powerState") @PrinterColumn(name = "status_powerState") val powerState: VirtualMachineModel.PowerState,
    @JsonProperty("overallStatus") val overallStatus: VirtualMachineModel.OverallStatus,
    @JsonProperty("netInfos") val netInfos: List<VirtualMachineModel.NetInfo>?,
)

fun VirtualMachineModel.toCrdSpec(): VirtualMachineSpec {
    return VirtualMachineSpec(
        uuid = this.uuid,
        name = this.name,
        platform = this.platform,
        template = this.isTemplate,
        host = this.host,

        adminId = this.adminId,
        studentId = this.studentId,
        teacherId = this.teacherId,
        experimental = this.isExperimental,
        experimentId = this.experimentId,
        applyId = this.applyId,

        cpu = this.cpu,
        memory = this.memory,
        diskNum = this.diskNum,
        diskSize = this.diskSize,
        osFullName = this.osFullName,
        powerState = null,
    )
}

fun VirtualMachineModel.toCrdStatus(): VirtualMachineStatus {
    return VirtualMachineStatus(
        powerState = this.powerState,
        overallStatus = this.overallStatus,
        netInfos = this.netInfos,
    )
}

@ControllerConfiguration(
    generationAwareEventProcessing = false,
)
class VirtualMachineReconciler(val client: KubernetesClient) : Reconciler<VirtualMachine>, Cleaner<VirtualMachine> {
    override fun reconcile(
        resource: VirtualMachine?,
        context: Context<VirtualMachine>?
    ): UpdateControl<VirtualMachine> {
        val vm = resource ?: return UpdateControl.noUpdate()
        if (vm.spec.deleted == true) return UpdateControl.noUpdate()
        val vmClient = newVMClient(vm.spec.platform)

        runBlocking {
            val vmModel = vmClient.getVM(vm.spec.uuid).getOrNull()
            if (vmModel == null) {
                vm.spec = vm.spec.copy(deleted = true)
            } else {
                vm.status = vmModel.toCrdStatus()
            }
        }
        if (vm.spec.deleted == true) return UpdateControl.updateResource(vm)

        // check the power
        if (vm.spec.powerState == VirtualMachineModel.PowerState.PoweredOn) {
            if (vm.status.powerState != VirtualMachineModel.PowerState.PoweredOn) {
                runBlocking {
                    vmClient.powerOnSync(vm.spec.uuid).getOrThrow()
                }
            }
        } else if (vm.spec.powerState == VirtualMachineModel.PowerState.PoweredOff) {
            if (vm.status.powerState != VirtualMachineModel.PowerState.PoweredOff) {
                runBlocking {
                    vmClient.powerOffSync(vm.spec.uuid).getOrThrow()
                }
            }
        }

        return UpdateControl.patchStatus(vm).rescheduleAfter(1000L)
    }

    override fun cleanup(resource: VirtualMachine?, context: Context<VirtualMachine>?): DeleteControl {
        val vm = resource ?: return DeleteControl.defaultDelete()
        val vmClient = newVMClient(vm.spec.platform)
        var exist = false
        runBlocking {
            if (vmClient.getVM(vm.spec.uuid).isSuccess) {
                exist = true
                vmClient.deleteVM(vm.spec.uuid).getOrThrow()
            }
        }
        return if (exist) DeleteControl.noFinalizerRemoval().rescheduleAfter(1000L)
        else DeleteControl.defaultDelete()
    }

}
