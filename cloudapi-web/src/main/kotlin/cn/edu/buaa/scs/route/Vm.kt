package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.*
import cn.edu.buaa.scs.controller.models.VirtualMachine as VirtualMachineResponse
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.kube.vmKubeClient
import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.model.VmApply
import cn.edu.buaa.scs.service.namespaceName
import cn.edu.buaa.scs.service.vm
import cn.edu.buaa.scs.vm.sfClient
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

            post("/vnc") {
                val vmId = call.getVmIdFromPath()
                val url = sfClient.createVNCConsole(vmId).getOrThrow()
                call.respond(VNCConsole(url = url))
            }

            delete {
                val vmId = call.getVmIdFromPath()
                call.vm.deleteVm(vmId)
                call.respond("OK")
            }

            route("/power") {
                patch {
                    val vmId = call.getVmIdFromPath()
                    call.vm.vmPower(
                        vmId,
                        call.request.queryParameters["action"] ?: throw BadRequestException("action is invalid"),
                    )
                    call.respond("OK")
                }
            }

        }

        route("/template") {

            get {
                call.respond(call.vm.getAllTemplates().map { convertVirtualMachineTemplateResponse(it) })
            }

            post {
                val req = call.receive<PostVmTemplateRequest>()
                call.respond(convertVirtualMachineTemplateResponse(call.vm.convertVMTemplate(req.uuid, req.isTemplate, req.crdId)))
            }
        }

        route("/templates") {
            get {
                call.respond(call.vm.getAllTemplates().map { convertVirtualMachineTemplateResponse(it) })
            }

        }
    }

    route("/experimentVms") {
        get {
            val experimentId = call.request.queryParameters["experimentId"]?.toInt()
            val managed = call.request.queryParameters["managed"]?.toBoolean() ?: false
            call.respond(call.vm.getExperimentVms(experimentId, managed).map { convertVirtualMachineResponse(it) })
        }
    }

    route("/personalVms") {
        get {
            call.respond(call.vm.getPersonalVms().map { convertVirtualMachineResponse(it) })
        }
    }

    route("/hosts") {
        get {
            call.respond(call.vm.getHosts())
        }
    }

    route("/vms") {
        get {
            call.respond(call.vm.adminGetAllVms().map { convertVirtualMachineResponse(it) })
        }

        route("/apply") {
            get {
                call.respond(
                    call.vm.getVmApplyList(call.parameters["expId"]?.toInt()).map { call.convertVmApplyResponse(it) })
            }

            post {
                val request = call.receive<CreateVmApplyRequest>()
                call.respond(call.convertVmApplyResponse(call.vm.createVmApply(request)))
            }

            route("/{applyId}") {

                fun ApplicationCall.getApplyIdFromPath(): String =
                    parameters["applyId"] ?: throw BadRequestException("apply id is invalid")

                get {
                    call.respond(call.convertVmApplyResponse(call.vm.getVmApply(call.getApplyIdFromPath())))
                }

                patch {
                    call.respond(
                        call.convertVmApplyResponse(
                            call.vm.handleApply(
                                call.getApplyIdFromPath(),
                                call.request.queryParameters["approve"]?.toBoolean() ?: false,
                                call.request.queryParameters["reply"] ?: ""
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

internal fun convertVirtualMachineResponse(vm: cn.edu.buaa.scs.kube.crd.v1alpha1.VirtualMachine): VirtualMachineResponse {
    val state = when {
        vm.status == null -> "creating"
        vm.metadata.deletionTimestamp != null -> "deleting"
        vm.status.powerState == VirtualMachine.PowerState.PoweredOn -> when (vm.spec.powerState) {
            VirtualMachine.PowerState.PoweredOff -> "shuttingDown"
            else -> "running"
        }

        vm.status.powerState == VirtualMachine.PowerState.PoweredOff -> when (vm.spec.powerState) {
            VirtualMachine.PowerState.PoweredOn -> "booting"
            else -> "stopped"
        }

        else -> "unknown"
    }.lowercase()

    val extraInfo = vm.spec.getVmExtraInfo()

    return VirtualMachineResponse(
        uuid = vm.status?.uuid,
        platform = vm.spec.platform,
        name = vm.spec.name,
        isTemplate = vm.spec.template,
        host = vm.status?.host,
        adminId = extraInfo.adminId,
        studentId = extraInfo.studentId,
        teacherId = extraInfo.teacherId,
        isExperimental = extraInfo.experimental,
        experimentId = extraInfo.experimentId,
        applyId = extraInfo.applyId,
        memory = vm.spec.memory,
        cpu = vm.spec.cpu,
        osFullName = vm.status?.osFullName,
        diskNum = vm.spec.diskNum,
        diskSize = vm.spec.diskSize,
        state = state,
        overallStatus = vm.status?.overallStatus?.value?.lowercase(),
        netInfos = vm.status?.netInfos?.map { VmNetInfo(it.macAddress, it.ipList) } ?: listOf(),
        id = vm.metadata.name,
    )
}

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
    overallStatus = vm.overallStatus.value,
)

internal fun ApplicationCall.convertVmApplyResponse(vmApply: VmApply) = CreateVmApplyResponse(
    id = vmApply.id,
    namePrefix = vmApply.namePrefix,
    studentId = convertSimpleUser(vmApply.studentId),
    teacherId = convertSimpleUser(vmApply.teacherId),
    applicant = convertSimpleUser(vmApply.applicant) ?: SimpleUser("null", "null"),
    experimentId = vmApply.experimentId,
    studentIdList = vmApply.studentIdList,
    cpu = vmApply.cpu,
    memory = vmApply.memory,
    diskSize = vmApply.diskSize,
    templateName = vmApply.getTemplateName(),
    description = vmApply.description,
    applyTime = vmApply.applyTime,
    status = vmApply.status,
    handleTime = vmApply.handleTime,
    expectedNum = vmApply.expectedNum,
    actualNum = vmApply.getActualNum(),
    dueTime = vmApply.dueTime,
    replyMsg = vmApply.replyMsg,
    process = this.vm.getVmApplyProcess(vmApply).let { (wanted, actual) -> VmApplyProcess(wanted, actual) },
)

internal fun convertExpVmInfo(vmApply: VmApply) = ExpVmInfo(
    status = vmApply.status,
    applyId = vmApply.id,
    expectedNum = vmApply.expectedNum,
    actualNum = vmKubeClient.inNamespace(vmApply.namespaceName()).list().items.count { !it.spec.deleted },
)
