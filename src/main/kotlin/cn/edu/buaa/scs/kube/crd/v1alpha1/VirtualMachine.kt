package cn.edu.buaa.scs.kube.crd.v1alpha1

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList
import io.fabric8.kubernetes.api.model.KubernetesResource
import io.javaoperatorsdk.operator.api.reconciler.*

class VirtualMachineList() : DefaultKubernetesResourceList<VirtualMachine>()

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VirtualMachineSpec(
    @JsonProperty("name") val name: String,
) : KubernetesResource

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VirtualMachineStatus(
    @JsonProperty("status") val status: String,
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
