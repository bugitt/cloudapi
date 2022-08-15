package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.*
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.model.VmApply
import cn.edu.buaa.scs.service.vm
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.vmRoute() {
    route("/vm") {

        fun ApplicationCall.getVmIdFromPath(): String =
            parameters["vmId"] ?: throw BadRequestException("vm id is invalid")

        route("/{vmId}") {

            get {
                val vmId = call.getVmIdFromPath()
                call.respond(convertVirtualMachineResponse(call.vm.getVmByUUID(vmId)))
            }

            route("/power") {
                patch {
                    val vmId = call.getVmIdFromPath()
                    call.vm.vmPower(
                        vmId,
                        call.request.queryParameters["action"] ?: "",
                        call.request.queryParameters["sync"]?.toBoolean() ?: false
                    )
                    call.respond("OK")
                }
            }

        }

        route("/template") {

            get {
                call.respond(call.vm.getAllTemplates().map { convertVirtualMachineResponse(it) })
            }
        }

        route("/templates") {
            get {
                call.respond(call.vm.getAllTemplates().map { convertVirtualMachineTemplateResponse(it) })
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

        route("/apply") {
            get {
                call.respond(call.vm.getVmApplyList().map { convertVmApplyResponse(it) })
            }

            post {
                val request = call.receive<CreateVmApplyRequest>()
                call.respond(convertVmApplyResponse(call.vm.createVmApply(request)))
            }

            route("/{applyId}") {

                fun ApplicationCall.getApplyIdFromPath(): String =
                    parameters["applyId"] ?: throw BadRequestException("apply id is invalid")

                get {
                    call.respond(convertVmApplyResponse(call.vm.getVmApply(call.getApplyIdFromPath())))
                }

                patch {
                    call.respond(
                        convertVmApplyResponse(
                            call.vm.handleApply(
                                call.getApplyIdFromPath(),
                                call.request.queryParameters["approve"]?.toBoolean() ?: false
                            )
                        )
                    )
                }

                route("/vms") {
                    patch {
                        val request = call.receive<PatchVmApplyVms>()
                        request.studentIdList?.let {
                            call.respond(
                                call.vm.addVmsToApply(call.getApplyIdFromPath(), it)
                            )
                        }
                    }

                    delete {
                        val request = call.receive<DeleteVmApplyVms>()
                        call.respond(
                            call.vm.deleteFromApply(
                                call.getApplyIdFromPath(),
                                request.studentId,
                                request.teacherId,
                                request.studentIdList,
                            )
                        )
                    }
                }
            }
        }
    }
}

internal fun convertVirtualMachineResponse(vm: VirtualMachine) = VirtualMachine(
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

internal fun convertVirtualMachineTemplateResponse(vm: VirtualMachine) = VirtualMachineTemplate(
    uuid = vm.uuid,
    platform = vm.platform,
    name = vm.name,
    host = vm.host,
    adminId = vm.adminId,
    studentId = vm.studentId,
    teacherId = vm.teacherId,
    isExperimental = vm.isExperimental,
    experimentId = vm.experimentId,
    memory = vm.memory,
    cpu = vm.cpu,
    osFullName = vm.osFullName,
    diskNum = vm.diskNum,
    diskSize = vm.diskSize,
    overallStatus = vm.overallStatus.value(),
)

internal fun convertVmApplyResponse(vmApply: VmApply) = CreateVmApplyResponse(
    id = vmApply.id,
    namePrefix = vmApply.namePrefix,
    studentId = vmApply.studentId,
    teacherId = vmApply.teacherId,
    experimentId = vmApply.experimentId,
    studentIdList = vmApply.studentIdList,
    cpu = vmApply.cpu,
    memory = vmApply.memory,
    diskSize = vmApply.diskSize,
    templateUuid = vmApply.templateUuid,
    description = vmApply.description,
    applyTime = vmApply.applyTime,
    status = vmApply.status,
    handleTime = vmApply.handleTime,
    expectedNum = vmApply.expectedNum,
    actualNum = vmApply.getActualNum(),
)