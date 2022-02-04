package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.FileResponse
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.File
import cn.edu.buaa.scs.model.FileType
import cn.edu.buaa.scs.service.file
import cn.edu.buaa.scs.service.id
import cn.edu.buaa.scs.utils.BASE_URL
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import java.net.URI
import java.net.URLEncoder

fun Route.fileRoute() {
    route("/file") {
        post {
            call.file.createOrUpdate().let {
                call.respond(convertFileResponse(it))
            }
        }

        route("/{fileId}") {

            fun ApplicationCall.getFileIdFromPath(): Int =
                parameters["fileId"]?.toInt()
                    ?: throw BadRequestException("file id is invalid")

            get {
                call.file.get(call.getFileIdFromPath()).let {
                    call.respond(convertFileResponse(it))
                }
            }

            route("/content") {

                // 下载文件
                get {
                    val file = File.id(call.getFileIdFromPath())
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(
                            ContentDisposition.Parameters.FileName,
                            @Suppress("BlockingMethodInNonBlockingContext")
                            URLEncoder.encode(file.name, "utf-8")
                        ).toString()
                    )
                    call.response.header(
                        "file-size",
                        file.size
                    )
                    call.respondOutputStream(
                        contentType = ContentType.parse(file.contentType),
                        status = HttpStatusCode.OK,
                        producer = call.file.fetchProducer(file)
                    )
                }
            }
        }

        route("/package") {
            get {
                val (fileType, involvedId) = try {
                    val query = call.request.queryParameters
                    val fileType = FileType.valueOf(query["type"] as String)
                    val involvedId = query["id"]!!.toInt()
                    Pair(fileType, involvedId)
                } catch (e: Exception) {
                    throw BadRequestException("please check your request parameters")
                }
                call.respondOutputStream(producer = call.file.packageDownload(fileType, involvedId))
            }
        }
    }


}


fun convertFileResponse(file: File): FileResponse {
    return FileResponse(
        id = file.id,
        name = file.name,
        uploadTime = file.uploadTime,
        fileType = file.fileType.name,
        fileSize = file.size,
        uploader = file.uploader,
        owner = file.owner,
        downloadLink = URI("$BASE_URL/api/v2/file/${file.id}/content"),
        createdAt = file.createdAt,
        updatedAt = file.updatedAt
    )
}