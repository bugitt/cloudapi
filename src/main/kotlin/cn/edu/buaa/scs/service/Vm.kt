package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.auth.hasAccessToStudent
import cn.edu.buaa.scs.controller.models.CreateVmApplyRequest
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.user
import cn.edu.buaa.scs.utils.userId
import cn.edu.buaa.scs.vm.CreateVmOptions
import cn.edu.buaa.scs.vm.VMTask
import cn.edu.buaa.scs.vm.vmClient
import io.ktor.application.*
import io.ktor.features.*
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.schema.ColumnDeclaring
import java.util.*

val ApplicationCall.vm
    get() = VmService.getSvc(this) { VmService(this) }

class VmService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<VmService>()

    private fun assertRead(uuid: String): VirtualMachine {
        val vm = mysql.virtualMachines.find { it.uuid.eq(uuid) }
            ?: throw NotFoundException("VirtualMachine($uuid) is not found")
        call.user().assertRead(vm)
        return vm
    }

    suspend fun vmPower(uuid: String, action: String, sync: Boolean = false) {
        assertRead(uuid)
        when (action) {
            "powerOn" -> if (sync) vmClient.powerOnSync(uuid).getOrThrow() else vmClient.powerOnAsync(uuid)
            "powerOff" -> if (sync) vmClient.powerOffSync(uuid).getOrThrow() else vmClient.powerOffAsync(uuid)
        }
    }

    fun getVmByUUID(uuid: String): VirtualMachine {
        return assertRead(uuid)
    }

    fun getVms(studentId: String?, teacherId: String?, experimentId: Int?): List<VirtualMachine> {
        val finalStudentId =
            if (studentId != null)
                if (call.user().hasAccessToStudent(studentId)) studentId
                else throw AuthorizationException("has no access to student($studentId)")
            else if (experimentId == null)
                call.userId()
            else null

        val finalTeacherId =
            if (teacherId != null)
                if (call.user().isAdmin() || call.user().isTeacher() && call.userId() == teacherId) teacherId
                else throw AuthorizationException("has no access to teacher($teacherId)")
            else if (call.user().isTeacher())
                call.userId()
            else null

        val finalExpId =
            if (experimentId != null) {
                call.user().assertWrite(Experiment.id(experimentId))
                experimentId
            } else {
                null
            }

        var condition: ColumnDeclaring<Boolean> = VirtualMachines.uuid.isNotNull()
        finalStudentId?.let { condition = condition.and(VirtualMachines.studentId.eq(it)) }
        finalTeacherId?.let { condition = condition.and(VirtualMachines.teacherId.eq(it)) }
        finalExpId?.let { condition = condition.and((VirtualMachines.experimentId.eq(it))) }
        return mysql.virtualMachines.filter { condition }.toList()
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
        // TODO: ??????????????????????????????
        return mysql.vmApplyList.find { it.id.eq(id) } ?: throw NotFoundException()
    }

    fun handleApply(id: String, approve: Boolean): VmApply {
        if (!call.user().isAdmin()) throw AuthorizationException()

        val vmApply = mysql.vmApplyList.find { it.id.eq(id) } ?: throw NotFoundException()
        return approveApply(vmApply, approve)
    }

    fun addVmsToApply(id: String, studentIdList: List<String>): VmApply {
        val vmApply = mysql.vmApplyList.find { it.id.eq(id) } ?: throw NotFoundException()
        call.user().assertWrite(vmApply)
        if (!vmApply.isApproved()) throw BadRequestException("the VMApply(${vmApply.id} is not approved")
        vmApply.studentIdList = vmApply.studentIdList.minus(studentIdList.toSet()) + studentIdList
        return approveApply(vmApply, true)
    }

    private fun approveApply(vmApply: VmApply, approve: Boolean): VmApply {
        if (approve) {
            vmApply.status = 1
        } else {
            vmApply.status = 2
        }
        vmApply.handleTime = System.currentTimeMillis()
        // generate vm creation tasks
        val tasks = generateVmCreationTasks(vmApply)
        mysql.useTransaction {
            mysql.vmApplyList.update(vmApply)
            if (vmApply.isApproved()) tasks.forEach { mysql.taskDataList.add(it) }
        }
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
                    if (vmApply.studentIdList.isEmpty()) listOf()
                    else mysql.virtualMachines.filter {
                        it.studentId.inList(vmApply.studentIdList) and
                                it.applyId.eq(vmApply.id)
                    }.toList()
                }
            }
            else -> listOf()
        }
        mysql.useTransaction {
            mysql.vmApplyList.update(vmApply)
            vmList.map { VMTask.vmDeleteTask(it.uuid) }.forEach { mysql.taskDataList.add(it) }
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
        mysql.vmApplyList.add(vmApply)
        return vmApply
    }

    fun getAllTemplate(): List<VirtualMachine> {
        return mysql.virtualMachines.filter { it.isTemplate.eq(true) }.toList()
    }

    private fun generateVmCreationTasks(vmApply: VmApply): List<TaskData> {
        val baseOptions = CreateVmOptions(
            name = vmApply.namePrefix,
            templateUuid = vmApply.templateUuid,
            memory = vmApply.memory,
            cpu = vmApply.cpu,
            diskSize = vmApply.diskSize,
            applyId = vmApply.id,
        )
        return when {
            vmApply.studentId.isNotBlank() && vmApply.studentId != "default" ->
                listOf(
                    baseOptions.copy(
                        name = "${vmApply.namePrefix}-${vmApply.studentId}",
                        studentId = vmApply.studentId
                    )
                )
            vmApply.teacherId.isNotBlank() && vmApply.teacherId != "default" ->
                listOf(
                    baseOptions.copy(
                        name = "${vmApply.namePrefix}-${vmApply.teacherId}",
                        teacherId = vmApply.teacherId
                    )
                )
            vmApply.experimentId != 0 -> {
                val experiment = Experiment.id(vmApply.experimentId)
                vmApply.studentIdList.map { studentId ->
                    baseOptions.copy(
                        name = "${vmApply.namePrefix}-$studentId",
                        studentId = studentId,
                        teacherId = experiment.course.teacher.id,
                        isExperimental = true,
                        experimentId = experiment.id
                    )
                }
            }
            else -> listOf()
        }
            .filter { !it.existInDb() }
            .map { VMTask.vmCreateTask(it) }
    }
}