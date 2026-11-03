package cn.edu.buaa.scs.vcenter

import cn.edu.buaa.scs.vm.ConfigVmOptions
import cn.edu.buaa.scs.vm.CreateVmOptions
import cn.edu.buaa.scs.model.VirtualMachine
import com.vmware.vim25.DuplicateNameFaultMsg
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.vcenterRouting(
    createVm: suspend (CreateVmOptions) -> Result<VirtualMachine> = VCenterWrapper::create,
) {
    routing {
        route("/api/v2/vcenter") {
            route("/vms") {
                get() {
                    call.respond(VCenterWrapper.getAllVms().getOrThrow())
                }

                post {
                    val result = createVm(call.receive())
                    val failure = result.exceptionOrNull()
                    if (failure is VmNameConflictException || failure is DuplicateNameFaultMsg) {
                        call.respond(
                            HttpStatusCode.Conflict,
                            mapOf(
                                "code" to "VM_NAME_CONFLICT",
                                "message" to (failure.message ?: "Virtual machine name already exists"),
                            ),
                        )
                    } else {
                        call.respond(result.getOrThrow())
                    }
                }

            }

            route("/hosts") {
                get {
                    call.respond(VCenterWrapper.getHosts())
                }
            }

            route("/vm/{uuid}") {
                fun ApplicationCall.getVmUuid() = parameters["uuid"]!!

                get {
                    call.respond(VCenterWrapper.getVM(call.getVmUuid()).getOrThrow())
                }

                delete {
                    VCenterWrapper.delete(call.getVmUuid()).getOrThrow()
                    call.respond("OK")
                }

                post("/powerOn") {
                    VCenterWrapper.powerOn(call.getVmUuid()).getOrThrow()
                    call.respond("OK")
                }

                post("/powerOff") {
                    VCenterWrapper.powerOff(call.getVmUuid()).getOrThrow()
                    call.respond("OK")
                }

                post("/config") {
                    val opt = call.receive<ConfigVmOptions>()
                    val vm = VCenterWrapper.configVM(call.getVmUuid(), opt).getOrThrow()
                    call.respond(vm)
                }

                post("/convertToTemplate") {
                    VCenterWrapper.convertVMToTemplate(call.getVmUuid()).getOrThrow()
                    call.respond("OK")
                }

                post("/ticket") {
                    val ticketResponse = VCenterWrapper.getWebTicket(call.getVmUuid())
                    call.respond(ticketResponse)
                }
            }

            route("/health") {
                get() {
                    call.respond("OK")
                }
            }
        }
    }
}
