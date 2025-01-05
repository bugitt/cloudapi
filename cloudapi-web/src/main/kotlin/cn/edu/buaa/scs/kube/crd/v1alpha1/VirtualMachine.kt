package cn.edu.buaa.scs.kube.crd.v1alpha1

import cn.edu.buaa.scs.kube.vmKubeClient
import cn.edu.buaa.scs.model.VirtualMachineExtraInfo
import cn.edu.buaa.scs.model.VmApply
import cn.edu.buaa.scs.model.vmApplyList
import cn.edu.buaa.scs.service.namespaceName
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.jsonMapper
import cn.edu.buaa.scs.utils.jsonReadValue
import cn.edu.buaa.scs.vm.CreateVmOptions
import cn.edu.buaa.scs.vm.newVMClient
import cn.edu.buaa.scs.utils.logger
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fkorotkov.kubernetes.newObjectMeta
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList
import io.fabric8.kubernetes.api.model.KubernetesResource
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.model.annotation.LabelSelector
import io.fabric8.kubernetes.model.annotation.PrinterColumn
import io.javaoperatorsdk.operator.api.reconciler.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.ktorm.entity.update
import java.util.*
import cn.edu.buaa.scs.model.VirtualMachine as VirtualMachineModel

class VirtualMachineList : DefaultKubernetesResourceList<VirtualMachine>()

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonDeserialize(using = JsonDeserializer.None::class)
data class VirtualMachineSpec(
    @JsonProperty("name") @LabelSelector val name: String,
    @JsonProperty("platform") val platform: String,
    @JsonProperty("template") val template: Boolean = false,


    @JsonProperty("extraInfo") val extraInfo: String,

    @JsonProperty("cpu") val cpu: Int,
    @JsonProperty("memory") val memory: Int, //MB
    @JsonProperty("diskNum") val diskNum: Int,
    @JsonProperty("diskSize") val diskSize: Long, // bytes
    @JsonProperty("powerState") @PrinterColumn(name = "spec_powerState") val powerState: VirtualMachineModel.PowerState?,

    @JsonProperty("deleted") val deleted: Boolean = false,
) : KubernetesResource {

    @JsonIgnore
    fun getVmExtraInfo(): VirtualMachineExtraInfo {
        return jsonReadValue(this.extraInfo)
    }

    fun toCreateVmOptions() = CreateVmOptions(
        name = this.name,
        extraInfo = this.getVmExtraInfo(),
        memory = this.memory,
        cpu = this.cpu,
        disNum = this.diskNum,
        diskSize = this.diskSize,
        powerOn = false,
    )

    fun toCrd(): VirtualMachine {
        val ns = this.getVmExtraInfo().applyId.lowercase()
        val vmSpec = this
        return VirtualMachine().apply {
            metadata = newObjectMeta {
                name = UUID.randomUUID().toString().lowercase()
                namespace = ns
            }
            this.spec = vmSpec
        }
    }

}

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonDeserialize(using = JsonDeserializer.None::class)
data class VirtualMachineStatus(
    @JsonProperty("uuid") val uuid: String,
    @JsonProperty("host") val host: String,
    @JsonProperty("osFullName") val osFullName: String,
    @JsonProperty("powerState") @PrinterColumn(name = "status_powerState") val powerState: VirtualMachineModel.PowerState,
    @JsonProperty("overallStatus") val overallStatus: VirtualMachineModel.OverallStatus,
    @JsonProperty("netInfos") val netInfos: List<VirtualMachineModel.NetInfo> = listOf(),
)

fun VirtualMachineModel.toCrdSpec(): VirtualMachineSpec {
    return VirtualMachineSpec(
        name = this.name,
        platform = this.platform,
        template = this.isTemplate,

        extraInfo = jsonMapper.writeValueAsString(VirtualMachineExtraInfo.valueFromVirtualMachine(this)),

        cpu = this.cpu,
        memory = this.memory,
        diskNum = this.diskNum,
        diskSize = this.diskSize,
        powerState = null,
    )
}

fun VirtualMachineModel.toCrdStatus(): VirtualMachineStatus {
    return VirtualMachineStatus(
        uuid = this.uuid,
        host = this.host,
        powerState = this.powerState,
        overallStatus = this.overallStatus,
        osFullName = this.osFullName,
        netInfos = this.netInfos,
    )
}

@ControllerConfiguration(
    generationAwareEventProcessing = true,
)
class VirtualMachineReconciler(val client: KubernetesClient) : Reconciler<VirtualMachine>, Cleaner<VirtualMachine> {
    companion object {
        val createVmProcessMutex = Mutex()
    }

    override fun reconcile(
        resource: VirtualMachine?,
        context: Context<VirtualMachine>?
    ): UpdateControl<VirtualMachine> {
        try {
            val vm = resource ?: return UpdateControl.noUpdate()
            val vmClient = newVMClient(vm.spec.platform)
            if (vm.spec.deleted) {
                val exist = runBlocking {
                    try {
                        if (vmClient.getVM(vm.status.uuid).isSuccess) {
                            vmClient.deleteVM(vm.status.uuid).getOrThrow()
                            true
                        } else {
                            false
                        }
                    } catch (e: NullPointerException) {
                        false
                    }
                }
                return if (exist) {
                    UpdateControl.noUpdate<VirtualMachine>().rescheduleAfter(10000L)
                } else {
                    UpdateControl.noUpdate()
                }
            }

            logger("vm-reconcile")().info { "Reconciling VirtualMachine: ${vm.spec.name}" }

            if (vm.status == null) {
                val vmModelResult = runBlocking { vmClient.getVMByName(vm.spec.name, vm.spec.getVmExtraInfo().applyId) }
                if (vmModelResult.isSuccess) {
                    vm.status = vmModelResult.getOrThrow().toCrdStatus()
                    return UpdateControl.patchStatus(vm).rescheduleAfter(10000L)
                } else {
                    val vmApply = VmApply.id(vm.spec.getVmExtraInfo().applyId)
                    if (vmApply == null || !vmApply.isApproved()) {
                        return UpdateControl.noUpdate<VirtualMachine>().rescheduleAfter(10000L)
                    }
                    val vmModel = runBlocking {
                        if (createVmProcessMutex.tryLock(vm)) {
                            try {
                                vmClient.createVM(vm.spec.toCreateVmOptions()).getOrThrow()
                            } finally {
                                createVmProcessMutex.unlock(vm)
                            }
                        } else {
                            null
                        }
                    }
                    return if (vmModel != null) {
                        vm.status = vmModel.toCrdStatus()
                        UpdateControl.patchStatus(vm).rescheduleAfter(10000L)
                    } else {
                        UpdateControl.noUpdate<VirtualMachine>().rescheduleAfter(10000L)
                    }
                }
            }

            assert(vm.status != null)

            // check if all done
            val vmApply = VmApply.id(vm.spec.getVmExtraInfo().applyId)
            if (vmApply != null && !vmApply.done) {
                val done = vmKubeClient
                    .inNamespace(vmApply.namespaceName())
                    .list()
                    .items
                    .all {
                        val extraInfo = it.spec.getVmExtraInfo()
                        extraInfo.initial && it.status != null
                    }
                if (done) {
                    vmApply.done = true
                    mysql.vmApplyList.update(vmApply)
                }
            }

            val vmUuid = vm.status.uuid

            runBlocking {
                val vmModel = vmClient.getVM(vmUuid).getOrNull()
                if (vmModel == null) {
                    vm.spec = vm.spec.copy(deleted = true)
                } else {
                    vm.status = vmModel.toCrdStatus()
                }
            }

            // check the power
            if (vm.spec.powerState == VirtualMachineModel.PowerState.PoweredOn) {
                if (vm.status.powerState != VirtualMachineModel.PowerState.PoweredOn) {
                    runBlocking {
                        vmClient.powerOnSync(vmUuid)
                    }
                }
            } else if (vm.spec.powerState == VirtualMachineModel.PowerState.PoweredOff) {
                if (vm.status.powerState != VirtualMachineModel.PowerState.PoweredOff) {
                    runBlocking {
                        vmClient.powerOffSync(vmUuid)
                    }
                }
            }

            return UpdateControl.patchStatus(vm).rescheduleAfter(10000L)
        } catch (e: Throwable) {
            logger("vm-reconcile")().error { "Reconciling virtual machine error: ${e.localizedMessage}" }
            return UpdateControl.patchStatus(resource!!).rescheduleAfter(10000L)
        }
    }

    override fun cleanup(resource: VirtualMachine?, context: Context<VirtualMachine>?): DeleteControl {
        val vm = resource ?: return DeleteControl.defaultDelete()
        if (vm.status == null) return DeleteControl.defaultDelete()

        val vmClient = newVMClient(vm.spec.platform)
        var exist = false
        runBlocking {
            if (vmClient.getVM(vm.status.uuid).isSuccess) {
                exist = true
                vmClient.deleteVM(vm.status.uuid).getOrThrow()
            }
        }
        return if (exist) DeleteControl.noFinalizerRemoval().rescheduleAfter(1000L)
        else DeleteControl.defaultDelete()
    }

}
