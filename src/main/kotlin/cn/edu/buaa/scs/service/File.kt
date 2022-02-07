package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertAdmin
import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.controller.models.FilePackageResponse
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.model.File
import cn.edu.buaa.scs.storage.S3
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.schedule.CommonScheduler
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
import java.io.*
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

    suspend fun createOrUpdate(): List<File> {
        val (fileParts, owner, fileType, involvedId, fileId) = parseFormData()
        // 校验参数
        if (fileId != null && fileParts.size != 1) {
            throw BadRequestException("当fileId不为空或0时, 仅允许上传单个文件")
        }
        if (fileType == FileType.Assignment && fileParts.size != 1) {
            throw BadRequestException("上传作业时，仅允许上传单个文件")
        }
        
        val service: IFileManageService = fileType.uploaderService()
        // check owner
        if (!service.checkOwner(owner, involvedId)) {
            throw BadRequestException("owner mismatch")
        }
        val handleFile: suspend (FilePart) -> File = { filePart ->
            val (name, storeName) = service.fixName(filePart.originalName, owner, involvedId)

            // upload
            val uploadResp = filePart.input().use {
                service.uploader().uploadFile(storeName, it, filePart.contentType, filePart.tmpFile?.length() ?: -1L)
            }

            val file = File {
                this.name = name
                this.storeType = StoreType.S3
                this.storeName = storeName
                this.storePath = service.storePath()
                this.uploadTime = uploadResp.uploadTime
                this.fileType = fileType
                this.involvedId = involvedId
                this.size = uploadResp.size
                this.uploader = call.userId()
                this.contentType = filePart.contentType
                this.owner = owner
            }

            call.user().assertAdmin(file)

            fileId?.let {
                file.id = it
                file.updatedAt = System.currentTimeMillis()
                mysql.files.update(file)
            } ?: run {
                file.createdAt = System.currentTimeMillis()
                file.updatedAt = System.currentTimeMillis()
                mysql.files.add(file)
            }
            // 清理 tempFile
            filePart.tmpFile?.delete()
            file
        }

        return CommonScheduler.multiCoroutinesProduce(fileParts.map { { handleFile(it) } }, Dispatchers.IO)
    }

    private data class FilePart(
        val originalName: String,
        val contentType: String,
        val input: () -> InputStream,
        val tmpFile: java.io.File? = null
    )

    private data class ParseFormDataResult(
        val fileParts: List<FilePart>,
        val owner: String,
        val fileType: FileType,
        val involvedId: Int,
        val fileId: Int?
    )

    private suspend fun parseFormData(): ParseFormDataResult {
        val fileParts = mutableListOf<FilePart>()
        var owner: String? = null
        var fileType: FileType? = null
        var involvedId: Int? = null
        var fileId: Int? = null
        val multiPart = call.receiveMultipart()
        try {
            multiPart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        assert(part.originalFileName != null)
                        fileParts.add(parseFilePart(part))
                    }
                    is PartData.FormItem ->
                        when (part.name) {
                            "owner" -> owner = part.value
                            "fileType" -> fileType = FileType.valueOf(part.value)
                            "involvedId" -> involvedId = part.value.toInt()
                            "fileId" -> fileId = part.value.toInt().let { if (it == 0) null else it }
                        }
                    else -> Unit
                }
            }
            return ParseFormDataResult(
                fileParts,
                owner!!,
                fileType!!,
                involvedId!!,
                fileId
            )
        } catch (e: Exception) {
            throw BadRequestException("please check your form-data request", e)
        }
    }


    private suspend fun parseFilePart(part: PartData.FileItem): FilePart {
        val originalName = part.originalFileName as String
        var contentType = part.contentType?.value ?: "application/octet-stream"
        if (contentType != "application/octet-stream")
            return FilePart(originalName, contentType, part.streamProvider)

        // use tika to detect file type
        val tmpFile = withContext(Dispatchers.IO) {
            val tmp = java.io.File.createTempFile(UUID.randomUUID().toString(), ".tmp")
            FileOutputStream(tmp).use { output ->
                part.streamProvider().copyTo(output)
            }
            contentType = Tika().detect(tmp)
            tmp
        }
        return FilePart(originalName, contentType, { BufferedInputStream(FileInputStream(tmpFile)) }, tmpFile)
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

    suspend fun `package`(fileType: FileType, involvedId: Int): FilePackageResponse {
        // check permission
        when (fileType) {
            FileType.Assignment ->
                call.user().assertWrite(Experiment.id(involvedId))
        }
        // get files
        val service = fileType.uploaderService()
        val (files, readme, zipFilename) = service.packageFiles(involvedId)
        val packageId = withContext(Dispatchers.IO) {
            val zipFile = java.io.File("${UUID.randomUUID()}.package.tmp")
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
            zipFile.name
        }
        return FilePackageResponse(packageId, zipFilename)
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