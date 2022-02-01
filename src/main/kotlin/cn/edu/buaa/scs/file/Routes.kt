package cn.edu.buaa.scs.file

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.File
import cn.edu.buaa.scs.model.files
import cn.edu.buaa.scs.service.file
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.getFormItem
import cn.edu.buaa.scs.utils.user
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.ktorm.dsl.eq
import org.ktorm.entity.find
import java.net.URLEncoder

fun Route.fileRoute() {
    route("/file/{fileId}") {
        fun ApplicationCall.getFile(): File =
            parameters["fileId"]?.toInt()?.let { fileId -> mysql.files.find { it.id eq fileId } }
                ?: throw BadRequestException("file id is invalid")

        get {
            call.getFile().let {
                call.user().assertRead(it)
                call.respond(it)
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
