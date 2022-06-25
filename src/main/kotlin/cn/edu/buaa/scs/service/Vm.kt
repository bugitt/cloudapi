package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.auth.hasAccessToStudent
import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.model.Experiment
import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.model.VirtualMachines
import cn.edu.buaa.scs.model.virtualMachines
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.user
import cn.edu.buaa.scs.utils.userId
import io.ktor.application.*
import io.ktor.features.*
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.isNotNull
import org.ktorm.entity.filter
import org.ktorm.entity.find
import org.ktorm.entity.toList
import org.ktorm.schema.ColumnDeclaring

val ApplicationCall.vm
    get() = VmService.getSvc(this) { VmService(this) }

class VmService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<VmService>()

    fun getVmByUUID(uuid: String): VirtualMachine {
        val vm = mysql.virtualMachines.find { it.uuid.eq(uuid) }
            ?: throw NotFoundException("VirtualMachine($uuid) is not found")
        call.user().assertRead(vm)
        return vm
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
}