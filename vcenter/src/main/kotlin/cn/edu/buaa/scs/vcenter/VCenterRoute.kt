package cn.edu.buaa.scs.vcenter

import cn.edu.buaa.scs.vm.ConfigVmOptions
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.vcenterRouting() {
    routing {
        route("/api/v2/vcenter") {
            route("/vms") {
                get() {
                    call.respond(VCenterWrapper.getAllVms().getOrThrow())
                }

                post {
                    val vm = VCenterWrapper.create(call.receive()).getOrThrow()
                    call.respond(vm)
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
            }

            route("/health") {
                get() {
                    call.respond("OK")
                }
            }
        }
    }
}
