package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.auth.authRead
import cn.edu.buaa.scs.cache.authRedis
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
import cn.edu.buaa.scs.vm.sangfor.SangforClient.getSangforHostsUsage
import cn.edu.buaa.scs.vm.vcenter.VCenterClient.getVcenterHostsUsage
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.schema.ColumnDeclaring
import java.util.*
import cn.edu.buaa.scs.utils.logger
import cn.edu.buaa.scs.vm.sangfor.SangforClient

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
        vmKubeClient.resource(vm).patch()
    }

    fun getVmByUUID(uuid: String): VirtualMachineCrd {
        return vmKubeClient.inAnyNamespace().withField("metadata.name", uuid).list().items.filterNot { it.spec.deleted }
            .firstOrNull()
            ?: throw NotFoundException("VirtualMachine($uuid) is not found")
    }

    fun deleteVm(id: String) {
        var vm = getVmByUUID(id)
        vm.spec = vm.spec.copy(deleted = true)
        vmKubeClient.resource(vm).patch()
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
            }
        }.filterNot { it.spec.deleted }
    }

    fun adminGetAllVms(): List<VirtualMachineCrd> {
        if (!call.user().isAdmin()) {
            throw AuthorizationException()
        }
        return vmKubeClient.inAnyNamespace().list().items.filterNot { it.spec.deleted }
    }

    fun getVmApplyList(expId: Int?): List<VmApply> {
        // fixme(loheagn): stupid logic
        val applyList = if (call.user().isAdmin()) {
            val applyList = mysql.vmApplyList.toList().toMutableList()
            applyList.sortByDescending { it.applyTime }
            applyList
        } else {
            val applyList = mutableListOf<VmApply>()
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
            applyList
        }
        applyList.sortByDescending { it.applyTime }
        return if (expId != null) applyList.filter { it.experimentId == expId } else applyList
    }

    // return (wanted, actual)
    fun getVmApplyProcess(vmApply: VmApply): Pair<Int, Int> {
        return when (vmApply.status) {
            0, 2 -> Pair(0, 0)
            1 -> {
                val vmList = vmKubeClient.inNamespace(vmApply.namespaceName()).list().items
                val wanted = if (vmApply.experimentId != 0) vmApply.studentIdList.size else 1
                val actual =
                    if (vmApply.done) wanted
                    else vmList.count { !it.spec.deleted && it.status != null && it.spec.getVmExtraInfo().initial }
                Pair(wanted, actual)
            }

            else -> throw BadRequestException("unknown status")
        }
    }

    fun getVmApply(id: String): VmApply {
        // TODO: 这里先不做鉴权，嘿嘿
        return mysql.vmApplyList.find { it.id.eq(id) } ?: throw NotFoundException()
    }

    suspend fun handleApply(id: String, approve: Boolean, replyMsg: String): VmApply {
        if (!call.user().isAdmin()) throw AuthorizationException()

        val vmApply = mysql.vmApplyList.find { it.id.eq(id) } ?: throw NotFoundException()
        return approveApply(vmApply, approve, replyMsg)
    }

    fun addVmsToApply(id: String, studentIdList: List<String>): VmApply {
        val vmApply = mysql.vmApplyList.find { it.id.eq(id) } ?: throw NotFoundException()
        val templateVM = mysql.virtualMachines.find { it.uuid.eq(vmApply.templateUuid) }
        var platform = "vcenter"
        templateVM?.let {
            platform = it.platform
        }
        call.user().assertWrite(vmApply)
        if (!vmApply.isApproved()) throw BadRequestException("the VMApply(${vmApply.id} is not approved")
        vmApply.namespaceName().ensureNamespace(kubeClient)
        // TODO: 这个函数给已有的vmApply添加更多的学生虚拟机，通过studentIdList直接在k8s中添加crd spec
        // vmApply.toVmCrdSpec返回一个包含若干等待创建的虚拟机spec的列表
        vmApply.toVmCrdSpec(false, platform, studentIdList).forEach { spec ->
            vmKubeClient
                .inNamespace(vmApply.namespaceName())
                .resource(spec.toCrd())
                .createOrReplace()
        }
        return vmApply
    }

    private suspend fun approveApply(vmApply: VmApply, approve: Boolean, replyMsg: String): VmApply {
        val logger = logger("approveApply")()
        if (approve) {
            vmApply.status = 1
        } else {
            vmApply.status = 2
        }
        vmApply.replyMsg = replyMsg
        vmApply.handleTime = System.currentTimeMillis()

        //// 原来的版本
        val templateVM = mysql.virtualMachines.find { it.uuid.eq(vmApply.templateUuid) }
        var platform = "vcenter"
        templateVM?.let {
            platform = it.platform
        }
        ////

        if (approve) {
            logger.info { "This apply is approved! Platform is ${platform}......" }
            val hostList = if (platform == "vcenter") {
                getVcenterHostsUsage().getOrThrow()

            } else {
                getSangforHostsUsage().getOrThrow()
            }
             // 所有平台主机列表
            val vmSpecList = vmApply.toVmCrdSpec(true, platform) // 一次性要创建的所有虚拟机列表！

            logger.info { "We have ${hostList.size} hosts alive and ${vmSpecList.size} vms to be create......" }

            // begin GA algo
            logger.info { "Begin running GeneticAlgorithm......" }
            val gA = GeneticAlgorithm(vmSpecList, hostList, 50, 0.3, 0.3, 5, 20)
            gA.evolve()
            val alloc = gA.getBestSolution().getAllocation()

            logger.info { "GeneticAlgorithm complete, result is: " }
            alloc.forEach { logger.info {hostList[it].hostId} }

            vmApply.namespaceName().ensureNamespace(kubeClient)
            vmSpecList.forEachIndexed { i, spec ->
                // TODO: 这个vmApply创建了很多个虚拟机spec（如果是实验用的，就会根据studentIdList创建非常多的spec），对于每个即将被创建的虚拟机：
                spec.hostId = hostList[alloc[i]].hostId // 给即将要被创建的vmSpec里写入分配到的hostID
                authRedis.setExpireKey(spec.name, hostList[alloc[i]].hostId, 3500)
                vmKubeClient
                    .inNamespace(vmApply.namespaceName())
                    .resource(spec.toCrd())
                    .createOrReplace()
            }
        }
        logger.info { "VM Apply Process finish! ALL VM create INTO CRD!" }
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
            this.applicant = call.userId()
            this.studentIdList = listOf()
            this.cpu = request.cpu
            this.memory = request.memory
            this.diskSize = request.diskSize
            this.templateUuid = request.templateUuid
            // TODO: 要求这个接口前端传来的request里也是templateName
//            this.templateName = request.templateName
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

    fun recordTemplateName2Uuid(vcenter_uuid: String, sangfor_uuid: String, name: String) {
        val sangforTemplate = TemplateUUID {
            platform = "sangfor"
            uuid = sangfor_uuid
            templateName = name
        }
        val vcenterTemplate = TemplateUUID {
            platform = "vcenter"
            uuid = vcenter_uuid
            templateName = name
        }
        mysql.templateUUIDs.add(sangforTemplate)
        mysql.templateUUIDs.add(vcenterTemplate)
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



fun VmApply.toVmCrdSpec(initial: Boolean = false, platform: String = "vcenter", extraStudentList: List<String>? = null): List<VirtualMachineSpec> {
    val vmApply = this
    val baseExtraInfo = VirtualMachineExtraInfo(
        applyId = vmApply.id,
        templateUuid = vmApply.templateUuid,
//         查数据表，查出这个platform中 这个模板名对应的模板uuid
//        templateUuid = mysql.templateUUIDs.find { it.platform.eq(platform) and it.templateName.eq(vmApply.templateName)}.uuid,
        initial = initial,
    )
    val baseSpec = VirtualMachineSpec(
        name = vmApply.namePrefix,
        memory = vmApply.memory,
        cpu = vmApply.cpu,
        diskNum = 1,
        diskSize = vmApply.diskSize,
        powerState = VirtualMachine.PowerState.PoweredOff,
        platform = platform,
        template = false,
        extraInfo = jsonMapper.writeValueAsString(baseExtraInfo),
    )
/*
根据传入的 extraStudentList 参数是否为空，分别处理不同的情况：
如果 extraStudentList 不为空，则表示需要创建多个学生专用的虚拟机。在这种情况下，对于列表中的每一个学生，都需要创建一个新的 VirtualMachineSpec 对象，并将其加入到返回值列表中。
如果 extraStudentList 为空，则根据传入的 VmApply 对象中的其他信息，创建一个或多个 VirtualMachineSpec 对象。具体来说，有以下几种情况：
如果 VmApply 对象中的 studentId 不为空，且不等于 "default"，则表示需要创建一个学生专用的虚拟机。
如果 VmApply 对象中的 teacherId 不为空，且不等于 "default"，则表示需要创建一个老师专用的虚拟机。
如果 VmApply 对象中的 experimentId 不为 0，则表示需要创建多个学生专用的虚拟机，且这些虚拟机都属于同一个实验。
 */
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
            // 如果是实验用的虚拟机，就会根据studentIdList参数，创建很多个虚拟机spec，返回给K8S CRD！
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
