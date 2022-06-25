package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.VmNetInfo
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.service.vm
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.vmRoute() {
    route("/vm") {

        fun ApplicationCall.getVmIdFromPath(): String =
            parameters["vmId"] ?: throw BadRequestException("vm id is invalid")

        route("/{vmId}") {

            get {
                val vmId = call.getVmIdFromPath()
                call.respond(convertVirtualMachineResponse(call.vm.getVmByUUID(vmId)))
            }

        }
    }

    route("/vms") {
        get {
            val studentId = call.request.queryParameters["studentId"]
            val teacherId = call.request.queryParameters["teacherId"]
            val experimentId = call.request.queryParameters["experimentId"]?.toInt()
            call.respond(call.vm.getVms(studentId, teacherId, experimentId).map { convertVirtualMachineResponse(it) })
        }
    }
}

internal fun convertVirtualMachineResponse(vm: VirtualMachine) = cn.edu.buaa.scs.controller.models.VirtualMachine(
    uuid = vm.uuid,
    platform = vm.platform,
    name = vm.name,
    isTemplate = vm.isTemplate,
    host = vm.host,
    adminId = vm.adminId,
    studentId = vm.studentId,
    teacherId = vm.teacherId,
    isExperimental = vm.isExperimental,
    experimentId = vm.experimentId,
    applyId = vm.applyId,
    memory = vm.memory,
    cpu = vm.cpu,
    osFullName = vm.osFullName,
    diskNum = vm.diskNum,
    diskSize = vm.diskSize,
    powerState = vm.powerState.value(),
    overallStatus = vm.overallStatus.value(),
    netInfos = vm.netInfos.map { VmNetInfo(it.macAddress, it.ipList) }
)