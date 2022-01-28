package cn.edu.buaa.scs.experiment

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.model.assignments
import cn.edu.buaa.scs.service.assignment
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.getFormItem
import cn.edu.buaa.scs.utils.user
import cn.edu.buaa.scs.utils.userId
import io.ktor.application.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.ktorm.dsl.eq
import org.ktorm.entity.find
import java.io.InputStream

fun Route.experimentRoute() {
    route("/experiment/{experimentId}") {

        fun ApplicationCall.getExpIdFromPath(): Int =
            parameters["experimentId"]?.toInt()
                ?: throw BadRequestException("experiment id is invalid")

        route("/assignment") {

            suspend fun ApplicationCall.parseUserAndFile(): Triple<String, String, InputStream> {
                return receiveMultipart().readAllParts().let { partDataList ->
                    val userId = getFormItem<PartData.FormItem>(partDataList, "owner")?.value ?: userId()

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
                        call.assignment.create(
                            experimentId,
                            userId,
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
                patch {
                    val experimentId = call.getExpIdFromPath()
                    val assignmentId = call.getAssignmentIdFromPath()
                    val assignment = mysql.assignments.find { it.id eq assignmentId }
                        ?: throw BadRequestException("assignment with assignmentId($assignmentId) not found")
                    call.parseUserAndFile()
                        .let { (_, filename, inputStream) ->

                            call.assignment.update(
                                assignment,
                                filename,
                                inputStream
                            )

                        }
                        .let { call.respond(it) }
                }

                get {
                    val assignmentId = call.getAssignmentIdFromPath()
                    val assignment = mysql.assignments.find { it.id eq assignmentId }
                        ?: throw NotFoundException("assignment with assignmentId($assignmentId) not found")
                    call.user().assertRead(assignment)
                    call.respond(assignment)
                }
            }
        }
    }
}