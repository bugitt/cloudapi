package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.authRead
import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.model.virtualMachines
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.user
import io.ktor.application.*
import io.ktor.features.*
import org.ktorm.dsl.eq
import org.ktorm.entity.find

val ApplicationCall.vm
    get() = VmService.getSvc(this) { VmService(this) }

class VmService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<VmService>()

    fun getVmByUUID(uuid: String): VirtualMachine {
        val vm = mysql.virtualMachines.find { it.uuid.eq(uuid) }
            ?: throw NotFoundException("VirtualMachine($uuid) is not found")
        call.user().authRead(vm)
        return vm
    }
}