package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.kube.crd.v1alpha1.toCrdSpec
import cn.edu.buaa.scs.kube.kubeClient
import cn.edu.buaa.scs.kube.vmKubeClient
import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.model.VirtualMachines
import cn.edu.buaa.scs.model.taskDataList
import cn.edu.buaa.scs.model.virtualMachines
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.task.Routine
import cn.edu.buaa.scs.task.RoutineTask
import cn.edu.buaa.scs.task.Task
import cn.edu.buaa.scs.utils.ensureNamespace
import kotlinx.coroutines.delay
import org.ktorm.dsl.*
import org.ktorm.entity.filter
import org.ktorm.entity.map
import org.ktorm.entity.toList

object VMRoutine : Routine {

    private val updateVmCrd = Routine.alwaysDo("vm-worker-update-crd") {
        val vmList = mutableListOf<VirtualMachine>()
        vmList.addAll(vmClient.getAllVMs().getOrThrow())
        vmList.addAll(sfClient.getAllVMs().getOrThrow())
        vmList.forEach { vmModel ->
            val ns = vmModel.applyId.lowercase()
            ns.ensureNamespace(kubeClient)
            val vmCrdList = vmKubeClient.inNamespace(ns).list().items
            val vmCrd = vmCrdList.find { it.spec.name == vmModel.name }
            if (vmCrd == null) {
                vmKubeClient.resource(vmModel.toCrdSpec().toCrd()).createOrReplace()
            } else {
                vmCrd.spec = vmCrd.spec.copy(extraInfo = vmModel.toCrdSpec().extraInfo)
                vmKubeClient.resource(vmCrd).patch()
            }
        }
        delay(4000L)
    }

    private val updateVMsToDatabase = Routine.alwaysDo("vm-worker-update-db") {
        val vmList = mutableListOf<VirtualMachine>()
        vmList.addAll(vmClient.getAllVMs().getOrThrow())
//        vmList.addAll(sfClient.getAllVMs().getOrThrow())
        val existedVmUUIDList = mysql.virtualMachines.map { it.uuid }.toSet()
        mysql.useTransaction {
            // update
            mysql.batchUpdate(VirtualMachines) {
                vmList.filter { it.uuid in existedVmUUIDList }.forEach { vm ->
                    item {
                        set(it.platform, vm.platform)
                        set(it.name, vm.name)
                        set(it.isTemplate, vm.isTemplate)
                        set(it.host, vm.host)
                        set(it.adminId, vm.adminId)
                        set(it.studentId, vm.studentId)
                        set(it.teacherId, vm.teacherId)
                        set(it.experimentId, vm.experimentId)
                        set(it.isExperimental, vm.isExperimental)
                        set(it.applyId, vm.applyId)
                        set(it.memory, vm.memory)
                        set(it.cpu, vm.cpu)
                        set(it.osFullName, vm.osFullName)
                        set(it.diskNum, vm.diskNum)
                        set(it.diskSize, vm.diskSize)
                        set(it.powerState, vm.powerState)
                        set(it.overallStatus, vm.overallStatus)
                        set(it.netInfos, vm.netInfos)
                        where { it.uuid eq vm.uuid }
                    }
                }
            }

            // create
            mysql.batchInsert(VirtualMachines) {
                vmList.filterNot { it.uuid in existedVmUUIDList }.forEach { vm ->
                    item {
                        set(it.uuid, vm.uuid)
                        set(it.platform, vm.platform)
                        set(it.name, vm.name)
                        set(it.isTemplate, vm.isTemplate)
                        set(it.host, vm.host)
                        set(it.adminId, vm.adminId)
                        set(it.studentId, vm.studentId)
                        set(it.teacherId, vm.teacherId)
                        set(it.experimentId, vm.experimentId)
                        set(it.isExperimental, vm.isExperimental)
                        set(it.applyId, vm.applyId)
                        set(it.memory, vm.memory)
                        set(it.cpu, vm.cpu)
                        set(it.osFullName, vm.osFullName)
                        set(it.diskNum, vm.diskNum)
                        set(it.diskSize, vm.diskSize)
                        set(it.powerState, vm.powerState)
                        set(it.overallStatus, vm.overallStatus)
                        set(it.netInfos, vm.netInfos)
                    }
                }
            }

            // 删除数据库中不应该存在的虚拟机
            mysql.delete(VirtualMachines) {
                it.uuid.notInList(vmList.map { vm -> vm.uuid })
            }
        }
        delay(4000L)
    }

    private val createVm = Routine.alwaysDo("create-virtual-machine") {
        mysql.taskDataList
            .filter { it.type.eq(Task.Type.VirtualMachine) and it.status.eq(Task.Status.UNDO) }
            .toList()
            .map { VMTask(it) }
            .forEach { it.process() }
    }

    private val deleteVm = Routine.alwaysDo("delete-virtual-machine") {
        mysql.virtualMachines.filter { it.lifetime.eq(VirtualMachine.Lifetime.DELETED) }
            .toList()
            .forEach {
                val vm = it
                vmClient.deleteVM(vm.uuid)
                    .onSuccess { vm.delete() }
            }
    }

    override val routineList: List<RoutineTask> = listOf(
        updateVmCrd,
        updateVMsToDatabase,
        createVm,
        deleteVm,
        // add more routines if needed
    )
}
