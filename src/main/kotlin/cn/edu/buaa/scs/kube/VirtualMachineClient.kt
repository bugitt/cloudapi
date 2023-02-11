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
        operator.register(VirtualMachineReconciler())
        operator.start()
    }

    fun get(name: String, namespace: String): VirtualMachine? {
        return client.inNamespace(namespace).withName(name).get()
    }
}
