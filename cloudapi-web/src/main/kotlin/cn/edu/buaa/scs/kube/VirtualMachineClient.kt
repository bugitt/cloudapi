package cn.edu.buaa.scs.kube

import cn.edu.buaa.scs.kube.crd.v1alpha1.*
import io.fabric8.kubernetes.client.dsl.MixedOperation
import io.fabric8.kubernetes.client.dsl.Resource
import io.javaoperatorsdk.operator.Operator

val vmKubeClient by lazy {
    kubeClient.resources(
        VirtualMachine::class.java,
        VirtualMachineList::class.java,
    ) as MixedOperation<VirtualMachine, VirtualMachineList, Resource<VirtualMachine>>
}

fun registerVirtualMachineOperator() {
    val operator = Operator(kubeClient)
//    operator.register(VirtualMachineReconciler(kubeClient))
//    operator.start()
}
