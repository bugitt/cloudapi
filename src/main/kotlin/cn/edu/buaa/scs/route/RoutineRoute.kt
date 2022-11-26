package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.kube.ContainerServiceDaemon
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.routineRoute() {
    route("/daemon") {

        route("/trigger") {

            route("/{action}") {

                fun ApplicationCall.getAction(): String {
                    return this.parameters["action"] ?: throw BadRequestException("No action specified")
                }

                post {
                    when (call.getAction()) {
                        "create-container-service" -> {
                            ContainerServiceDaemon.asyncDoOnce()
                        }
                    }
                    call.respond("OK")
                }
            }

        }
    }
}