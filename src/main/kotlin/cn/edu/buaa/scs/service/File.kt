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
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.tika.Tika
import org.ktorm.dsl.eq
import org.ktorm.entity.add
import org.ktorm.entity.find
import org.ktorm.entity.update
import java.io.*
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

val ApplicationCall.file: FileService
    get() = FileService.getSvc(this) { FileService(this) }

class FileService(val call: ApplicationCall) : IService {

    companion object : IService.Caller<FileService>() {
        val scsosS3 = S3("scsos")
        val packageResult = ConcurrentHashMap<String, Boolean>()
    }

    /**
     * 简单的对象存储上传
     * @return String 可供访问的文件URL
     */
    suspend fun scsosCreate(): String {
        val multipart = call.receiveMultipart()
        var filePart: PartData.FileItem? = null
        multipart.forEachPart { part ->
            when (part) {
                is PartData.FileItem -> {
                    filePart = part
                }

                else -> part.dispose()
            }
        }
        if (filePart == null) {
            throw BadRequestException("No file part found")
        }
        val originName = filePart?.originalFileName ?: ""
        val fileName = withContext(Dispatchers.IO) {
            val fileName = URLEncoder.encode("${UUID.randomUUID()}-$originName", "UTF-8")
            filePart?.streamProvider?.invoke()?.use { input ->
                scsosS3.uploadFile(
                    "public/$fileName",
                    input,
                    filePart?.contentType?.value ?: "application/octet-stream"
                )
            } ?: throw BadRequestException("No file part found")
            fileName
        }
        // TODO 落库
        return "https://scs.buaa.edu.cn/scsos/public/$fileName"
    }

    sealed interface IFileManageService {

        fun manager(): S3

        // return filename and storeName
        fun fixName(originalName: String?, ownerId: String, involvedId: Int): Pair<String, String>

        fun checkPermission(ownerId: String, involvedId: Int): Boolean

        fun storePath(): String

        fun beforeUploadFile(involvedEntity: IEntity, filePart: FilePart) {}

        fun beforeCreateOrUpdate(involvedEntity: IEntity, file: File) {}

        // after create file
        fun afterCreateOrUpdate(involvedEntity: IEntity, file: File) {}

        suspend fun packageFiles(involvedId: Int, fileIdList: List<Int>?): PackageResult {
            throw NotImplementedError()
        }
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

        val service: IFileManageService = fileType.manageService()
        // check owner
        if (!service.checkPermission(owner, involvedId)) {
            throw BadRequestException("owner mismatch")
        }
        val involvedEntity = fileType.getInvolvedEntity(involvedId)

        data class HandleFileCreateOrUpdate(
            val file: File,
            val action: (File) -> Unit,
        )

        val handleFile: suspend (FilePart) -> HandleFileCreateOrUpdate = { filePart ->
            service.beforeUploadFile(involvedEntity, filePart)

            val (name, storeName) = service.fixName(filePart.originalName, owner, involvedId)

            // upload
            val uploadResp = filePart.input().use {
                service.manager().uploadFile(storeName, it, filePart.contentType, filePart.tmpFile?.length() ?: -1L)
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

            HandleFileCreateOrUpdate(file) { innerFile ->
                service.beforeCreateOrUpdate(involvedEntity, innerFile)
                fileId?.let {
                    innerFile.id = it
                    innerFile.updatedAt = System.currentTimeMillis()
                    mysql.files.update(innerFile)
                } ?: run {
                    innerFile.createdAt = System.currentTimeMillis()
                    innerFile.updatedAt = System.currentTimeMillis()
                    mysql.files.add(innerFile)
                }
                service.afterCreateOrUpdate(involvedEntity, innerFile)
                // 清理 tempFile
                filePart.tmpFile?.delete()
            }
        }

        val handlerList =
            CommonScheduler.multiCoroutinesProduceSync(fileParts.map { { handleFile(it) } }, Dispatchers.IO)
        mysql.useTransaction {
            handlerList.forEach { it.action(it.file) }
        }
        return handlerList.map { it.file }
    }

    data class FilePart(
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

        suspend fun parseFilePart(part: PartData.FileItem): FilePart {
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
            if (owner.isNullOrBlank()) owner = call.request.queryParameters["owner"]
            if (fileType == null) fileType = call.request.queryParameters["fileType"]?.let { FileType.valueOf(it) }
            if (involvedId == null) involvedId = call.request.queryParameters["involvedId"]?.toInt()
            if (fileId == null)
                fileId = call.request.queryParameters["fileId"]?.toInt()?.let { if (it == 0) null else it }
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


    fun get(fileId: Int): File {
        val file = mysql.files.find { it.id eq fileId }
            ?: throw NotFoundException("assignment($fileId) not found")
        call.user().assertRead(file)
        return file
    }

    /**
     * 本方法不包含鉴权操作
     */
    internal suspend fun deleteFileFromStorage(file: File) {
        file.fileType.manageService().manager().deleteFile(file.storeName)
    }

    suspend fun fetchProducer(file: File): suspend OutputStream.() -> Unit {
        call.user().assertRead(file)
        val service = file.fileType.manageService()
        val inputStream = service.manager().getFile(file.storeName)
        return { inputStream.use { it.copyTo(this) } }
    }

    fun getPackageResult(packageId: String): Boolean {
        return packageResult[packageId]
            ?: throw cn.edu.buaa.scs.error.BadRequestException("no such packageId($packageId)")
    }

    suspend fun `package`(fileType: FileType, involvedId: Int, fileIdList: List<Int>?) {
        // check permission
        when (fileType) {
            FileType.Assignment ->
                // 老师和助教才能打包作业
                call.user().assertWrite(Experiment.id(involvedId))

            FileType.CourseResource ->
                // 对课程有读权限的，都可以打包下载课程资源
                call.user().assertRead(Course.id(involvedId))

            FileType.ExperimentResource ->
                call.user().assertRead(Experiment.id(involvedId))

            FileType.AssignmentReview ->
                Unit
        }
        // get files
        val service = fileType.manageService()
        val (files, readme, zipFilename) = service.packageFiles(involvedId, fileIdList)
        val packageId = "${UUID.randomUUID()}.package.tmp"
        packageResult[packageId] = false
        call.respond(FilePackageResponse(packageId, zipFilename))
        withContext(Dispatchers.IO) {
            val zipFile = java.io.File(packageId)
            zipFile.createNewFile()
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
                files.forEach { file ->
                    zipOut.putNextEntry(ZipEntry(file.name))
                    service.manager().getFile(file.storeName).use { input ->
                        input.copyTo(zipOut)
                    }
                }

                zipOut.putNextEntry(ZipEntry("README"))
                zipOut.write(readme.toByteArray())
            }
            packageResult[packageId] = true
        }
    }

    private fun FileType.manageService(): IFileManageService =
        when (this) {
            FileType.Assignment -> call.assignment
            FileType.CourseResource -> call.courseResource
            FileType.ExperimentResource -> call.experiment
            FileType.AssignmentReview -> call.assignmentReview
        }

    private fun FileType.getInvolvedEntity(involvedId: Int): IEntity =
        when (this) {
            FileType.Assignment -> Assignment.id(involvedId)
            FileType.CourseResource -> Course.id(involvedId)
            FileType.ExperimentResource -> Experiment.id(involvedId)
            FileType.AssignmentReview -> Assignment.id(involvedId)
        }
}

fun File.Companion.id(id: Int): File {
    return mysql.files.find { it.id eq id }
        ?: throw BusinessException("find file($id) from database error")
}