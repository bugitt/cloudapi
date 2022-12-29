package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.*
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.File
import cn.edu.buaa.scs.model.FileType
import cn.edu.buaa.scs.service.S3Uploader
import cn.edu.buaa.scs.service.file
import cn.edu.buaa.scs.service.id
import cn.edu.buaa.scs.utils.BASE_URL
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLEncoder
import java.nio.file.Files

fun Route.fileRoute() {
    route("/scsos") {
        post {
            val fileUrl = call.file.scsosCreate()
            call.respond(fileUrl)
        }
    }

    route("/file") {

        post("convert") {
            call.file.convertS3ToLocal()
            call.respond("OK")
        }

        post {
            call.file.createOrUpdate().let {
                call.respond(UploadFileResponse(it.map { file -> convertFileResponse(file) }))
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
                val (packageId, packageName) = try {
                    val query = call.request.queryParameters
                    val packageId = query["packageId"]!!
                    val packageName = query["packageName"]!!
                    Pair(packageId, packageName)
                } catch (e: Exception) {
                    throw BadRequestException("please check your request parameters")
                }
                val file = java.io.File(packageId)
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        @Suppress("BlockingMethodInNonBlockingContext")
                        URLEncoder.encode(packageName, "utf-8")
                    ).toString()
                )
                call.respondFile(file)
                // delete tmp file
                withContext(Dispatchers.IO) {
                    Files.delete(file.toPath())
                }

            }

            post {
                val req = call.receive<FilePackageRequest>()
                call.file.`package`(FileType.valueOf(req.fileType), req.involvedId, req.fileIdList)
            }

            route("/result") {
                get {
                    val packageId = call.request.queryParameters["packageId"]
                        ?: throw BadRequestException("please check your request parameters")
                    if (call.file.getPackageResult(packageId)) {
                        call.respond(HttpStatusCode.Created)
                    } else {
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }

        route("/s3") {
            route("/uploader") {
                post {
                    call.respond(
                        convertS3User(call.file.createS3Uploader())
                    )
                }
            }
        }
    }


}


internal fun convertFileResponse(file: File): FileResponse {
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
        updatedAt = file.updatedAt,
        contentType = file.contentType,
        involveId = file.involvedId
    )
}

internal fun convertS3User(s3Uploader: S3Uploader): S3Config {
    return S3Config(
        accessKey = s3Uploader.accessKey,
        secretKey = s3Uploader.secretKey,
        endpoint = s3Uploader.endpoint,
        scheme = s3Uploader.scheme,
        bucket = s3Uploader.bucket,
        key = s3Uploader.key,
        region = s3Uploader.region,
    )
}

