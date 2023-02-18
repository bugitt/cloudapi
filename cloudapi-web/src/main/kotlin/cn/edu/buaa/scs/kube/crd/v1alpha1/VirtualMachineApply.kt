package cn.edu.buaa.scs.kube.crd.v1alpha1

import cn.edu.buaa.scs.kube.vmKubeClient
import cn.edu.buaa.scs.model.VmApply
import cn.edu.buaa.scs.vm.CreateVmOptions
import cn.edu.buaa.scs.vm.newVMClient
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList
import io.fabric8.kubernetes.model.annotation.PrinterColumn
import io.javaoperatorsdk.operator.api.reconciler.*
import kotlinx.coroutines.runBlocking

class VirtualMachineApplyList() : DefaultKubernetesResourceList<VirtualMachineApply>()

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = JsonDeserializer.None::class)
data class VirtualMachineApplySpec(
    @JsonProperty("id") val id: String,
    @JsonProperty("status") val status: Int, // 0: 还未处理; 1: 允许; 2: 拒绝
    @JsonProperty("entityList") val entityList: MutableList<VirtualMachineApplyVmEntity>,
    @JsonProperty("extraEntityList") val extraEntityList: MutableList<VirtualMachineApplyVmEntity>? = mutableListOf(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = JsonDeserializer.None::class)
data class VirtualMachineApplyStatus(
    @JsonProperty("creating") @PrinterColumn val creating: Boolean,
    @JsonProperty("done") @PrinterColumn val done: Boolean,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = JsonDeserializer.None::class)
data class VirtualMachineApplyVmEntity(
    @JsonProperty("createVmOptions") val createVmOptions: CreateVmOptions,
    @JsonProperty("platform") val platform: String,
    @JsonProperty("vmUuid") val vmUuid: String? = null,
    @JsonProperty("crdName") val crdName: String? = null,
)

@ControllerConfiguration(
    generationAwareEventProcessing = false,
)
class VirtualMachineApplyReconciler() : Reconciler<VirtualMachineApply>, Cleaner<VirtualMachineApply> {
    override fun reconcile(
        resource: VirtualMachineApply?,
        context: Context<VirtualMachineApply>?
    ): UpdateControl<VirtualMachineApply> {
        val vmApply = resource ?: return UpdateControl.noUpdate()

        if (vmApply.status == null) {
            vmApply.status = VirtualMachineApplyStatus(
                creating = false,
                done = false,
            )
            return UpdateControl.patchStatus(vmApply).rescheduleAfter(1)
        }

        if (vmApply.spec.status != 1) {
            return UpdateControl.noUpdate()
        }

        // then, we need to create the virtual machines
        if (!vmApply.status.creating) {
            vmApply.status = vmApply.status.copy(creating = true)
            return UpdateControl.patchStatus(vmApply).rescheduleAfter(1)
        }

        val entityListList = listOfNotNull(vmApply.spec.entityList, vmApply.spec.extraEntityList)
        if (entityListList.all { it.all { it.vmUuid != null && it.crdName != null } }) {
            vmApply.status = vmApply.status.copy(
                done = true,
                creating = false,
            )
            return UpdateControl.patchStatus(vmApply)
        }

        listOfNotNull(vmApply.spec.entityList, vmApply.spec.extraEntityList).forEach { entityList ->
            val index = entityList.indexOfFirst { it.vmUuid == null || it.crdName == null }
            if (index == -1) {
                return@forEach
            }

            val entity = entityList[index]
            val vmClient = newVMClient(entity.platform)
            if (entity.vmUuid == null) {
                val vm = runBlocking {
                    vmClient.getVMByName(entity.createVmOptions.name, vmApply.spec.id).getOrNull()
                        ?: vmClient.createVM(entity.createVmOptions).getOrNull()
                }

                if (vm != null) {
                    entityList[index] = entity.copy(vmUuid = vm.uuid)
                    return UpdateControl.updateResource(vmApply).rescheduleAfter(1)
                }
            }

            if (entity.crdName == null) {
                val vmCrd = vmKubeClient.withName(entity.vmUuid!!).get()
                if (vmCrd != null) {
                    entityList[index] = entity.copy(crdName = vmCrd.metadata.name)
                    return UpdateControl.updateResource(vmApply).rescheduleAfter(1)
                }
            }
        }

        return UpdateControl.noUpdate<VirtualMachineApply>().rescheduleAfter(500L)
    }

    override fun cleanup(resource: VirtualMachineApply?, context: Context<VirtualMachineApply>?): DeleteControl {
        // do nothing
        return DeleteControl.defaultDelete()
    }

}
