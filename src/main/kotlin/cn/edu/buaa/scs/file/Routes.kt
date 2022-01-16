package cn.edu.buaa.scs.file

import cn.edu.buaa.scs.auth.assertAssignment
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.experiment.getAssignmentFile
import cn.edu.buaa.scs.model.FileType
import cn.edu.buaa.scs.storage.files
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.userId
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import org.ktorm.dsl.eq
import org.ktorm.entity.find

fun Route.fileRoute() {
    route("/file/{fileId}") {
        fun ApplicationCall.getFileIdFromPath(): Int =
            parameters["fileId"]?.toInt()
                ?: throw BadRequestException("file id is invalid")

        get {
            val fileId = call.getFileIdFromPath()
            val file =
                mysql.files.find { it.id eq fileId } ?: throw BadRequestException("file with id($fileId) not found")

            val producer = when (file.fileType) {
                FileType.Assignment -> {
                    assertAssignment(call.userId(), file.involvedId)
                    getAssignmentFile(file.storeName)
                }
            }
            // TODO: 文件名
            // FIXME: 文件名
            call.respondOutputStream(status = HttpStatusCode.OK, producer = producer)
        }
    }
}
