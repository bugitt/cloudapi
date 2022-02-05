package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertAdmin
import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.S3
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.user
import cn.edu.buaa.scs.utils.userId
import cn.edu.buaa.scs.utils.value
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.content.*
import io.ktor.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.tika.Tika
import org.ktorm.dsl.eq
import org.ktorm.entity.add
import org.ktorm.entity.find
import org.ktorm.entity.update
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

val ApplicationCall.file: FileService
    get() = FileService(this)

class FileService(val call: ApplicationCall) {

    interface IFileManageService {

        fun uploader(): S3

        // return filename and storeName
        fun fixName(originalName: String?, ownerId: String, involvedId: Int): Pair<String, String>

        fun checkOwner(ownerId: String, involvedId: Int): Boolean

        fun storePath(): String

        suspend fun packageFiles(involvedId: Int): PackageResult
    }

    data class PackageResult(
        val files: List<File>,
        val readme: String,
        val zipFilename: String,
    )

    suspend fun createOrUpdate(): File {
        val req = parseFormData()
        val (tmpFile, contentType) = detectContentType(req.filePart)
        val service: IFileManageService = req.fileType.uploaderService()
        // check owner
        if (!service.checkOwner(req.owner, req.involvedId)) {
            throw BadRequestException("owner mismatch")
        }
        val (name, storeName) = service.fixName(req.filePart.originalFileName, req.owner, req.involvedId)

        // upload
        val input = tmpFile?.let { FileInputStream(it) } ?: req.filePart.streamProvider()
        val uploadResp = input.use {
            service.uploader().uploadFile(storeName, it, contentType, tmpFile?.length() ?: -1L)
        }

        val file = File {
            this.name = name
            this.storeType = StoreType.S3
            this.storeName = storeName
            this.storePath = service.storePath()
            this.uploadTime = uploadResp.uploadTime
            this.fileType = req.fileType
            this.involvedId = req.involvedId
            this.size = uploadResp.size
            this.uploader = call.userId()
            this.contentType = contentType
            this.owner = req.owner
        }

        call.user().assertAdmin(file)

        req.fileId?.let {
            file.id = it
            file.updatedAt = System.currentTimeMillis()
            mysql.files.update(file)
        } ?: run {
            file.createdAt = System.currentTimeMillis()
            file.updatedAt = System.currentTimeMillis()
            mysql.files.add(file)
        }
        // 清理 tempFile
        tmpFile?.delete()
        return file
    }

    private data class ParseFormDataResult(
        val filePart: PartData.FileItem,
        val owner: String,
        val fileType: FileType,
        val involvedId: Int,
        val fileId: Int?
    )

    private suspend fun parseFormData(): ParseFormDataResult {
        var filePart: PartData.FileItem? = null
        var owner: String? = null
        var fileType: FileType? = null
        var involvedId: Int? = null
        var fileId: Int? = null
        val multiPart = call.receiveMultipart()
        try {
            multiPart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem ->
                        if (part.name == "file")
                            filePart = part
                    is PartData.FormItem ->
                        when (part.name) {
                            "owner" -> owner = part.value
                            "fileType" -> fileType = FileType.valueOf(part.value)
                            "involvedId" -> involvedId = part.value.toInt()
                            "fileId" -> fileId = part.value.toInt()
                        }
                    else -> Unit
                }
            }
            return ParseFormDataResult(
                filePart!!,
                owner!!,
                fileType!!,
                involvedId!!,
                fileId
            )
        } catch (e: Exception) {
            throw BadRequestException("please check your form-data request")
        }
    }


    private suspend fun detectContentType(part: PartData.FileItem): Pair<java.io.File?, String> {
        var contentType = part.contentType?.value ?: "application/octet-stream"
        if (contentType != "application/octet-stream") return Pair(null, contentType)

        // use tika to detect file type
        val tmpFile = withContext(Dispatchers.IO) {
            val tmp = java.io.File.createTempFile(UUID.randomUUID().toString(), ".tmp")
            FileOutputStream(tmp).use { output ->
                part.streamProvider().copyTo(output)
            }
            contentType = Tika().detect(tmp)
            tmp
        }
        return Pair(tmpFile, contentType)
    }


    fun get(fileId: Int): File {
        val file = mysql.files.find { it.id eq fileId }
            ?: throw NotFoundException("assignment($fileId) not found")
        call.user().assertRead(file)
        return file
    }

    suspend fun fetchProducer(file: File): suspend OutputStream.() -> Unit {
        call.user().assertRead(file)
        val service = file.fileType.uploaderService()
        val inputStream = service.uploader().getFile(file.storeName)
        return { inputStream.use { it.copyTo(this) } }
    }

    suspend fun packageDownload(fileType: FileType, involvedId: Int): java.io.File {
        // check permission
        when (fileType) {
            FileType.Assignment ->
                call.user().assertWrite(Experiment.id(involvedId))
        }
        // get files
        val service = fileType.uploaderService()
        val (files, readme, zipFilename) = service.packageFiles(involvedId)
        return withContext(Dispatchers.IO) {
            val dirname = "package/${UUID.randomUUID()}"
            java.io.File(dirname).mkdirs()
            val zipFile = java.io.File("$dirname/$zipFilename")
            zipFile.createNewFile()
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
                files.forEach { file ->
                    zipOut.putNextEntry(ZipEntry(file.name))
                    service.uploader().getFile(file.storeName).use { input ->
                        input.copyTo(zipOut)
                    }
                }

                zipOut.putNextEntry(ZipEntry("README"))
                zipOut.write(readme.toByteArray())
            }
            zipFile
        }
    }

    private fun FileType.uploaderService(): IFileManageService =
        when (this) {
            FileType.Assignment -> call.assignment
        }
}

fun File.Companion.id(id: Int): File {
    return mysql.files.find { it.id eq id }
        ?: throw BusinessException("find file($id) from database error")
}