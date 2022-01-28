package cn.edu.buaa.scs.file

import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.File
import cn.edu.buaa.scs.model.files
import cn.edu.buaa.scs.service.file
import cn.edu.buaa.scs.storage.mysql
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import org.ktorm.dsl.eq
import org.ktorm.entity.find

fun Route.fileRoute() {
    route("/file/{fileId}") {
        fun ApplicationCall.getFile(): File =
            parameters["fileId"]?.toInt()?.let { fileId -> mysql.files.find { it.id eq fileId } }
                ?: throw BadRequestException("file id is invalid")

        get {
            val file = call.getFile()
            // TODO: 文件名
            // FIXME: 文件名
            call.respondOutputStream(status = HttpStatusCode.OK, producer = call.file.fetchProducer(file))
        }
    }
}
