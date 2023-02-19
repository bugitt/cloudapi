package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.auth.authRead
import cn.edu.buaa.scs.auth.hasAccessToStudent
import cn.edu.buaa.scs.controller.models.CreateVmApplyRequest
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.kube.crd.v1alpha1.VirtualMachineSpec
import cn.edu.buaa.scs.kube.crd.v1alpha1.VirtualMachine as VirtualMachineCrd
import cn.edu.buaa.scs.kube.kubeClient
import cn.edu.buaa.scs.kube.vmKubeClient
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.*
import cn.edu.buaa.scs.vm.*
import com.fkorotkov.kubernetes.newObjectMeta
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.schema.ColumnDeclaring
import java.util.*

val ApplicationCall.vm
    get() = VmService.getSvc(this) { VmService(this) }

class VmService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<VmService>()

    fun vmPower(uuid: String, action: String) {
        val vm = getVmByUUID(uuid)
        when (action.lowercase()) {
            "poweron" -> vm.spec = vm.spec.copy(powerState = VirtualMachine.PowerState.PoweredOn)
            "poweroff" -> vm.spec = vm.spec.copy(powerState = VirtualMachine.PowerState.PoweredOff)
        }
    }

    fun getVmByUUID(uuid: String): VirtualMachineCrd {
        return vmKubeClient.inAnyNamespace().withField("metadata.name", uuid).list().items.filterNot { it.spec.deleted }
            .firstOrNull()
            ?: throw NotFoundException("VirtualMachine($uuid) is not found")
    }

    fun deleteVm(id: String) {
        val vm = getVmByUUID(id)
        vmKubeClient.resource(vm).delete()
    }

    fun getPersonalVms(): List<VirtualMachineCrd> {
        val vmApplyList =
            mysql.vmApplyList.filter { ((it.studentId eq call.userId()) or (it.teacherId eq call.userId())) and (it.experimentId eq 0) }
                .toList()
        return vmApplyList.flatMap { vmApply ->
            val vmList = vmKubeClient.inNamespace(vmApply.namespaceName()).list().items
            vmList.filter {
                val extraInfo = it.spec.getVmExtraInfo()
                extraInfo.studentId == call.userId() || extraInfo.teacherId == call.userId()
            }.filterNot { it.spec.deleted }
        }
    }

    fun getExperimentVms(experimentId: Int?, managed: Boolean): List<VirtualMachineCrd> {
        val expIdList = if (experimentId != null) {
            val experiment = Experiment.id(experimentId)
            call.user().assertRead(experiment)
            listOf(experimentId)
        } else {
            if (managed) {
                call.user().getAllManagedExperimentIdList()
            } else {
                call.user().getAllExperimentIdListAsStudent()
            }
        }
        val vmApplyList =
            if (expIdList.isEmpty()) listOf()
            else mysql.vmApplyList.filter { it.experimentId.inList(expIdList) }.toList()
        return vmApplyList.flatMap { vmApply ->
            val vmList = vmKubeClient.inNamespace(vmApply.namespaceName()).list().items
            if (managed) vmList
            else vmList.filter {
                val extraInfo = it.spec.getVmExtraInfo()
                extraInfo.studentId == call.userId() || extraInfo.teacherId == call.userId()
            }.filterNot { it.spec.deleted }
        }
    }

    fun adminGetAllVms(): List<VirtualMachineCrd> {
        if (!call.user().isAdmin()) {
            throw AuthorizationException()
        }
        return vmKubeClient.inAnyNamespace().list().items.filterNot { it.spec.deleted }
    }

    fun getVmApplyList(): List<VmApply> {
        val applyList = mutableListOf<VmApply>()
        if (call.user().isAdmin()) return mysql.vmApplyList.toList()
        applyList += mysql.vmApplyList.filter {
            it.studentId.eq(call.userId())
                .or(it.teacherId.eq(call.userId()))
        }.toList()
        val experimentIdList = call.user().getAllManagedExperimentIdList()
        if (experimentIdList.isNotEmpty()) {
            applyList += mysql.vmApplyList.filter {
                it.experimentId.inList(experimentIdList)
            }.toList()
        }
        return applyList
    }

    fun getVmApply(id: String): VmApply {
        // TODO: 这里先不做鉴权，嘿嘿
        return mysql.vmApplyList.find { it.id.eq(id) } ?: throw NotFoundException()
    }

    fun handleApply(id: String, approve: Boolean, replyMsg: String): VmApply {
        if (!call.user().isAdmin()) throw AuthorizationException()

        val vmApply = mysql.vmApplyList.find { it.id.eq(id) } ?: throw NotFoundException()
        return approveApply(vmApply, approve, replyMsg)
    }

    fun addVmsToApply(id: String, studentIdList: List<String>): VmApply {
        val vmApply = mysql.vmApplyList.find { it.id.eq(id) } ?: throw NotFoundException()
        call.user().assertWrite(vmApply)
        if (!vmApply.isApproved()) throw BadRequestException("the VMApply(${vmApply.id} is not approved")
        vmApply.namespaceName().ensureNamespace(kubeClient)
        vmApply.toVmCrdSpec(false, studentIdList).forEach { spec ->
            vmKubeClient
                .inNamespace(vmApply.namespaceName())
                .resource(spec.toCrd())
                .createOrReplace()
        }
        return vmApply
    }

    private fun approveApply(vmApply: VmApply, approve: Boolean, replyMsg: String): VmApply {
        if (approve) {
            vmApply.status = 1
        } else {
            vmApply.status = 2
        }
        vmApply.replyMsg = replyMsg
        vmApply.handleTime = System.currentTimeMillis()

        vmApply.namespaceName().ensureNamespace(kubeClient)
        vmApply.toVmCrdSpec(true).forEach { spec ->
            vmKubeClient
                .inNamespace(vmApply.namespaceName())
                .resource(spec.toCrd())
                .createOrReplace()
        }
        mysql.vmApplyList.update(vmApply)
        return vmApply
    }

    fun deleteFromApply(id: String, studentId: String?, teacherId: String?, studentIdList: List<String>?): VmApply {
        val vmApply = mysql.vmApplyList.find { it.id.eq(id) } ?: throw NotFoundException()
        call.user().assertWrite(vmApply)

        val vmList: List<VirtualMachine> = when {
            studentId != null -> {
                vmApply.expectedNum = 0
                mysql.virtualMachines.filter { it.studentId.eq(studentId) and it.applyId.eq(vmApply.id) }.toList()
            }

            teacherId != null -> {
                vmApply.expectedNum = 0
                mysql.virtualMachines.filter { it.studentId.eq(teacherId) and it.applyId.eq(vmApply.id) }.toList()
            }

            studentIdList != null -> {
                if (studentIdList.isEmpty()) listOf()
                else {
                    vmApply.studentIdList = vmApply.studentIdList.minus(studentIdList.toSet())
                    vmApply.expectedNum = vmApply.studentIdList.size
                    mysql.virtualMachines.filter {
                        it.studentId.inList(studentIdList) and
                                it.applyId.eq(vmApply.id)
                    }.toList()
                }
            }

            else -> listOf()
        }
        mysql.useTransaction {
            mysql.vmApplyList.update(vmApply)
            vmList.forEach {
                it.markDeleted()
                mysql.virtualMachines.update(it)
            }
        }
        return vmApply
    }

    fun createVmApply(request: CreateVmApplyRequest): VmApply {
        val vmApply = VmApply {
            this.id = UUID.randomUUID().toString()
            this.namePrefix = request.namePrefix
            this.studentId = "default"
            this.teacherId = "default"
            this.experimentId = 0
            this.studentIdList = listOf()
            this.cpu = request.cpu
            this.memory = request.memory
            this.diskSize = request.diskSize
            this.templateUuid = request.templateUuid
            this.description = request.description
            this.applyTime = System.currentTimeMillis()
            this.status = 0
            this.handleTime = 0L
            this.dueTime = request.dueTime
        }
        when {
            request.studentId != null ->
                if (call.user().isAdmin()
                    || call.user().isStudent() && call.userId() == request.studentId
                ) {
                    vmApply.studentId = request.studentId
                    vmApply.expectedNum = 1
                } else {
                    throw AuthorizationException()
                }

            request.teacherId != null ->
                if (call.user().isAdmin()
                    || call.user().isTeacher() && call.userId() == request.teacherId
                ) {
                    vmApply.teacherId = request.teacherId
                    vmApply.expectedNum = 1
                } else {
                    throw AuthorizationException()
                }

            request.experimentId != null && request.experimentId != 0 && request.studentIdList != null -> {
                val experiment = Experiment.id(request.experimentId)
                if (call.user().isAdmin()
                    || call.user().isCourseAssistant(experiment.course) || call.user()
                        .isCourseTeacher(experiment.course)
                ) {
                    vmApply.experimentId = request.experimentId
                    vmApply.studentIdList = request.studentIdList
                    vmApply.expectedNum = request.studentIdList.size
                }
            }
        }

        vmApply.namespaceName().ensureNamespace(kubeClient)

        mysql.vmApplyList.add(vmApply)
        return vmApply
    }

    fun getAllTemplates(): List<VirtualMachine> {
        val user = call.user()
        var condition: ColumnDeclaring<Boolean> = VirtualMachines.isTemplate.eq(true)
        val publicCondition: ColumnDeclaring<Boolean> =
            VirtualMachines.studentId.eq("default").and(VirtualMachines.teacherId.eq("default"))
        condition = when {
            user.isAdmin() -> condition

            user.isTeacher() -> {
                var subCondition: ColumnDeclaring<Boolean> =
                    VirtualMachines.teacherId.eq(user.id).and(VirtualMachines.studentId.eq("default"))
                val studentIdList = user.getAllAssistantIdList()
                if (studentIdList.isNotEmpty()) subCondition =
                    subCondition.or(VirtualMachines.studentId.inList(studentIdList))
                condition.and(subCondition.or(publicCondition))
            }

            else -> {
                var subCondition: ColumnDeclaring<Boolean> = VirtualMachines.studentId.eq(user.id)
                val teacherIdList = user.getAllAssistantIdList()
                if (teacherIdList.isNotEmpty())
                    subCondition = subCondition.or(
                        VirtualMachines.teacherId.inList(teacherIdList) and (VirtualMachines.studentId.eq("default"))
                    )
                condition.and(subCondition.or(publicCondition))
            }
        }
        return mysql.virtualMachines.filter { condition }.toList()
    }

    suspend fun convertVMToTemplate(uuid: String, name: String): VirtualMachine {
        var vm = mysql.virtualMachines.find { it.uuid.eq(uuid) } ?: throw NotFoundException("VM not found")
        call.user().assertWrite(vm)
        // 检查是否已经关机
        if (vm.powerState.value.lowercase() != "poweredoff") {
            throw BadRequestException("VM is not powered off")
        }
        if (vm.isTemplate) {
            throw BadRequestException("VM is already a template")
        }
        // 检查template名称是否重复
        if (mysql.virtualMachines.exists { it.isTemplate.eq(true) and it.name.eq(name) }) {
            throw BadRequestException("template name already exists")
        }
        // convert machine into template
        vm = vmClient.convertVMToTemplate(uuid).getOrThrow()
        // config vm template
        val (adminId, teacherId, studentId) = when {
            call.user().isAdmin() -> Triple("default", "default", "default")
            call.user().isTeacher() -> Triple("default", call.userId(), "default")
            else -> Triple("default", "default", call.userId())
        }
        return vmClient.configVM(
            vm.uuid,
            adminId = adminId,
            teacherId = teacherId,
            studentId = studentId,
        ).getOrThrow()
    }
}

suspend fun DefaultWebSocketServerSession.sshWS(uuid: String) {
    val vm =
        mysql.virtualMachines.find { it.uuid.eq(uuid) } ?: throw NotFoundException("virtual machine ($uuid) not found")
    call.user().authRead(vm)
    val username = if (vm.name.startsWith("kube")) "root" else sshConfig.defaultUsername

    val sshSession = SSH.vmGetSSH(vm, username)
        ?: throw cn.edu.buaa.scs.error.BadRequestException("can not connect to virtual machine ($uuid)")
    sshSession.use { ssh ->
        val launch = launch {
            ssh.readCommand(incoming)
        }
        while (launch.isActive) {
            ssh.processOutput(outgoing)
            flush()
            if (!ssh.isActive()) {
                break
            }
            delay(20)
        }
    }
}

fun VmApply.namespaceName(): String {
    return this.id.lowercase()
}

fun VmApply.toVmCrdSpec(initial: Boolean = false, extraStudentList: List<String>? = null): List<VirtualMachineSpec> {
    val vmApply = this
    val baseExtraInfo = VirtualMachineExtraInfo(
        applyId = vmApply.id,
        templateUuid = vmApply.templateUuid,
        initial = initial,
    )
    val baseSpec = VirtualMachineSpec(
        name = vmApply.namePrefix,
        memory = vmApply.memory,
        cpu = vmApply.cpu,
        diskNum = 1,
        diskSize = vmApply.diskSize,
        powerState = VirtualMachine.PowerState.PoweredOff,
        platform = "vcenter",
        template = false,
        extraInfo = jsonMapper.writeValueAsString(baseExtraInfo),
    )
    return if (extraStudentList != null) {
        val experiment = Experiment.id(vmApply.experimentId)
        extraStudentList.map { studentId ->
            baseSpec.copy(
                name = "${vmApply.namePrefix}-$studentId",
                extraInfo = jsonMapper.writeValueAsString(
                    baseExtraInfo.copy(
                        studentId = studentId,
                        teacherId = experiment.course.teacher.id,
                        experimental = true,
                        experimentId = experiment.id,
                    )
                )
            )
        }
    } else when {
        vmApply.studentId.isNotBlank() && vmApply.studentId != "default" ->
            listOf(
                baseSpec.copy(
                    name = "${vmApply.namePrefix}-${vmApply.studentId}",
                    extraInfo = jsonMapper.writeValueAsString(
                        baseExtraInfo.copy(
                            studentId = vmApply.studentId,
                        )
                    )
                )
            )

        vmApply.teacherId.isNotBlank() && vmApply.teacherId != "default" ->
            listOf(
                baseSpec.copy(
                    name = "${vmApply.namePrefix}-${vmApply.teacherId}",
                    extraInfo = jsonMapper.writeValueAsString(
                        baseExtraInfo.copy(
                            teacherId = vmApply.teacherId,
                        )
                    )
                )
            )

        vmApply.experimentId != 0 -> {
            val experiment = Experiment.id(vmApply.experimentId)
            vmApply.studentIdList.map { studentId ->
                baseSpec.copy(
                    name = "${vmApply.namePrefix}-$studentId",
                    extraInfo = jsonMapper.writeValueAsString(
                        baseExtraInfo.copy(
                            studentId = studentId,
                            teacherId = experiment.course.teacher.id,
                            experimental = true,
                            experimentId = experiment.id,
                        )
                    )
                )
            }
        }

        else -> listOf()
    }
}
