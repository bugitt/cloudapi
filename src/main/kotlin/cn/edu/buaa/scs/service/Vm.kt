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
import cn.edu.buaa.scs.vm.vmClient
import io.ktor.application.*
import io.ktor.features.*
import org.ktorm.dsl.*
import org.ktorm.entity.add
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.toList
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
            else if (call.user().isStudent())
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
        applyList += mysql.vmApplyList.filter {
            it.experimentId.inList(call.user().getAllManagedExperimentIdList())
        }.toList()
        return applyList
    }

    fun getVmApply(id: String): VmApply {
        // TODO: 这里先不做鉴权，嘿嘿
        return mysql.vmApplyList.find { it.id.eq(id) } ?: throw NotFoundException()
    }

    fun handleApply(id: String, approve: Boolean): VmApply {
        val vmApply = mysql.vmApplyList.find { it.id.eq(id) } ?: throw NotFoundException()
        if (approve) {
            vmApply.status = 1
        } else {
            vmApply.status = 2
        }
        vmApply.handleTime = System.currentTimeMillis()
        vmApply.flushChanges()
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
}