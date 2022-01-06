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

fun Route.experimentRoute() {
    route("/experiment/{experimentId}") {
        route("/assignment") {

            /**
             * 第一次上传作业
             */
            post {
                val experimentId = call.parameters["experimentId"]?.toInt()
                    ?: throw BadRequestException("experiment id is invalid")
                call.receiveMultipart().readAllParts().let { partDataList ->
                    val userId = getFormItem<PartData.FormItem>(partDataList, "userId")?.value ?: call.userId()

                    // check permission
                    call.assertPermission(userId == call.userId()) {
                        assertExperiment(call.userId(), experimentId)
                    }

                    val fileItem = getFormItem<PartData.FileItem>(partDataList, "file")
                        ?: throw BadRequestException("can not parse file item")
                    createAssignment(
                        experimentId,
                        userId,
                        call.userId(),
                        fileItem.originalFileName ?: "",
                        fileItem.streamProvider()
                    )
                }.let {
                    call.respond(it)
                }
            }
        }
    }
}