package cn.edu.buaa.scs.experiment

import cn.edu.buaa.scs.auth.assertExperiment
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.utils.assertPermission
import cn.edu.buaa.scs.utils.getFormItem
import cn.edu.buaa.scs.utils.userId
import io.ktor.application.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.io.InputStream

fun Route.experimentRoute() {
    route("/experiment/{experimentId}") {

        fun ApplicationCall.getExpIdFromPath(): Int =
            parameters["experimentId"]?.toInt()
                ?: throw BadRequestException("experiment id is invalid")

        route("/assignment") {

            suspend fun ApplicationCall.parseUserAndFile(): Triple<String, String, InputStream> {
                val experimentId = getExpIdFromPath()
                return receiveMultipart().readAllParts().let { partDataList ->
                    val userId = getFormItem<PartData.FormItem>(partDataList, "owner")?.value ?: userId()

                    // check permission
                    assertPermission(userId == userId()) {
                        assertExperiment(userId(), experimentId)
                    }

                    val fileItem = getFormItem<PartData.FileItem>(partDataList, "file")
                        ?: throw BadRequestException("can not parse file item")
                    Triple(userId, fileItem.originalFileName ?: "", fileItem.streamProvider())
                }
            }

            /**
             * 第一次上传作业
             */
            post {
                val experimentId = call.getExpIdFromPath()
                call.parseUserAndFile()
                    .let { (userId, filename, inputStream) ->

                        // check permission
                        call.assertPermission(userId == call.userId()) {
                            assertExperiment(call.userId(), experimentId)
                        }

                        createAssignment(
                            experimentId,
                            userId,
                            call.userId(),
                            filename,
                            inputStream
                        )

                    }
                    .let { call.respond(it) }
            }

            route("/{assignmentId}") {

                fun ApplicationCall.getAssignmentIdFromPath(): Int =
                    parameters["assignmentId"]?.toInt()
                        ?: throw BadRequestException("assignmentId id is invalid")

                /**
                 * 重复提交作业
                 */
                put {
                    val experimentId = call.getExpIdFromPath()
                    val assignmentId = call.getAssignmentIdFromPath()
                    call.parseUserAndFile()
                        .let { (userId, filename, inputStream) ->

                            // check permission
                            call.assertPermission(userId == call.userId()) {
                                assertExperiment(call.userId(), experimentId)
                            }

                            updateAssigmentFile(
                                assignmentId,
                                userId,
                                call.userId(),
                                filename,
                                inputStream
                            )

                        }
                        .let { call.respond(it) }
                }
            }
        }
    }
}