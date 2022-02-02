package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.FileResponse
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.File
import cn.edu.buaa.scs.model.FileType
import cn.edu.buaa.scs.model.files
import cn.edu.buaa.scs.service.file
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.ktorm.dsl.eq
import org.ktorm.entity.find
import java.net.URI
import java.net.URLEncoder

fun Route.fileRoute() {
    route("/file") {
        post {
            val readChannel = call.receiveChannel()
            val headers = call.request.headers
            call.file.createOrUpdate(
                readChannel,
                call.headerString("file-name"),
                call.headerString("file-owner"),
                try {
                    FileType.valueOf(call.headerString("file-type"))
                } catch (e: Exception) {
                    throw BadRequestException("invalid file-type, think about ${FileType.values()}")
                },
                call.headerInt("involved-id"),
                call.headerString("content-type"),
                call.headerIntOrNull("file-id")
            )
        }

        route("/{fileId}") {
            fun ApplicationCall.getFile(): File =
                parameters["fileId"]?.toInt()?.let { fileId -> mysql.files.find { it.id eq fileId } }
                    ?: throw BadRequestException("file id is invalid")

            fun ApplicationCall.getFileIdFromPath(): Int =
                parameters["fileId"]?.toInt()
                    ?: throw BadRequestException("file id is invalid")

            get {
                call.file.get(call.getFileIdFromPath()).let {
                    call.respond(convertFileResponse(it))
                }
            }

            patch {
                val fileItem = call.receiveMultipart().readAllParts().let { partDataList ->
                    getFormItem<PartData.FileItem>(partDataList, "file")
                        ?: throw BadRequestException("can not parse file item")
                }
                call.file.update(call.getFile(), fileItem.originalFileName, fileItem.streamProvider()).let {
                    call.respond(it)
                }
            }

            route("/content") {
                get {
                    val file = call.getFile()
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(
                            ContentDisposition.Parameters.FileName,
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