package cn.edu.buaa.scs.kube

import cn.edu.buaa.scs.kube.crd.v1alpha1.VirtualMachine
import cn.edu.buaa.scs.kube.crd.v1alpha1.VirtualMachineList
import cn.edu.buaa.scs.kube.crd.v1alpha1.VirtualMachineReconciler
import io.fabric8.kubernetes.client.dsl.MixedOperation
import io.fabric8.kubernetes.client.dsl.Resource
import io.javaoperatorsdk.operator.Operator

object VirtualMachineClient {
    private val client by lazy {
        kubeClient.resources(
            VirtualMachine::class.java,
            VirtualMachineList::class.java,
        ) as MixedOperation<VirtualMachine, VirtualMachineList, Resource<VirtualMachine>>
    }

    fun registerVirtualMachineOperator() {
        val operator = Operator(kubeClient)
        operator.register(VirtualMachineReconciler(kubeClient))
        operator.start()
    }

    fun list(): List<VirtualMachine> {
        return client.list().items
    }

    fun get(name: String): VirtualMachine? {
        return client.withName(name).get()
    }

    fun updateStatus(vm: VirtualMachine) {
        client.resource(vm).patchStatus()
    }

    fun createOrReplace(vm: VirtualMachine) {
        client.resource(vm).createOrReplace()
    }

    fun delete(vm: VirtualMachine) {
        client.resource(vm).delete()
    }

    fun delete(name: String) {
        client.withName(name).delete()
    }
}
