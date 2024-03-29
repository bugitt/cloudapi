package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.application
import cn.edu.buaa.scs.auth.assertAdmin
import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.config.Constant
import cn.edu.buaa.scs.controller.models.FilePackageResponse
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.model.File
import cn.edu.buaa.scs.storage.file.FileManager
import cn.edu.buaa.scs.storage.file.S3
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.*
import cn.edu.buaa.scs.utils.schedule.CommonScheduler
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator
import org.apache.commons.compress.archivers.zip.UnixStat
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.tika.Tika
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.add
import org.ktorm.entity.find
import org.ktorm.entity.update
import java.io.*
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

data class S3Uploader(
    val endpoint: String,
    val scheme: String,
    val accessKey: String,
    val secretKey: String,
    val region: String,
    val bucket: String,
    val key: String,
)

val ApplicationCall.file: FileService
    get() = FileService.getSvc(this) { FileService(this) }

class FileService(val call: ApplicationCall) : IService {

    companion object : IService.Caller<FileService>() {
        val scsosS3 = S3("scsos")
        val packageResult = ConcurrentHashMap<String, Boolean>()

        val s3Endpoint by lazy {
            application.getConfigString("s3.common.endpoint")
        }

        val s3Region by lazy {
            application.getConfigString("s3.common.region")
        }

        val s3Scheme by lazy {
            application.getConfigString("s3.common.scheme")
        }
    }

    /**
     * 简单的对象存储上传
     * @return String 可供访问的文件URL
     */
    suspend fun scsosCreate(overrideName: Boolean): String {
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
            val fileName = if (overrideName) {
                "${UUID.randomUUID()}.${originName.getFileExtension()}"
            } else {
                URLEncoder.encode("${UUID.randomUUID()}-$originName", "UTF-8")
            }
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
        return "${Constant.baseUrl}/scsos/public/$fileName"
    }

    suspend fun convertS3ToLocal() {
        if (!call.user().isAdmin()) return
        val fileId = call.request.queryParameters["fileId"]?.toInt() ?: throw BadRequestException("No file id found")
        mysql.files.find { it.id.eq(fileId).and(it.storeType.eq("S3")) }?.let { f ->
            val inputStream = f.inputStreamSuspend()
            inputStream.use {
                val fileManager = FileManager.buildFileManager("local", f.storePath)
                fileManager.uploadFile(f.storeName, it, f.contentType, f.size)
            }
            f.storeType = "LOCAL"
            f.flushChanges()
        } ?: throw NotFoundException("File not found")
    }

    sealed interface FileDecorator {

        fun manager(): FileManager

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

        suspend fun s3UploaderAliasAndUserGroup(key: String): S3Uploader {
            throw NotImplementedError()
        }
    }

    data class PackageResult(
        val files: List<File>,
        val readme: String,
        val zipFilename: String,
    )

    suspend fun createOrUpdate(): List<File> {
        val (fileParts, _, owner, fileType, involvedId, fileId, decorator) = parseFormData()

        val involvedEntity = fileType.getInvolvedEntity(involvedId)

        data class HandleFileCreateOrUpdate(
            val file: File,
            val action: (File) -> Unit,
        )

        val handleFile: suspend (FilePart) -> HandleFileCreateOrUpdate = { filePart ->
            decorator.beforeUploadFile(involvedEntity, filePart)

            val (name, storeName) = decorator.fixName(filePart.originalName, owner, involvedId)

            // upload
            val uploadResp = filePart.input().use {
                decorator.manager().uploadFile(storeName, it, filePart.contentType, filePart.tmpFile?.length() ?: -1L)
            }

            val file = File {
                this.name = name
                this.storeType = decorator.manager().name()
                this.storeName = storeName
                this.storePath = decorator.storePath()
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
                decorator.beforeCreateOrUpdate(involvedEntity, innerFile)
                fileId?.let {
                    innerFile.id = it
                    innerFile.updatedAt = System.currentTimeMillis()
                    mysql.files.update(innerFile)
                } ?: run {
                    innerFile.createdAt = System.currentTimeMillis()
                    innerFile.updatedAt = System.currentTimeMillis()
                    mysql.files.add(innerFile)
                }
                decorator.afterCreateOrUpdate(involvedEntity, innerFile)
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
        val originalName: String,
        val owner: String,
        val fileType: FileType,
        val involvedId: Int,
        val fileId: Int?,
        val decorator: FileDecorator,
    ) {
        fun fixName(): Pair<String, String> {
            return decorator.fixName(originalName, owner, involvedId)
        }
    }

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
                part.dispose()
                tmp
            }
            return FilePart(originalName, contentType, { BufferedInputStream(FileInputStream(tmpFile)) }, tmpFile)
        }

        val fileParts = mutableListOf<FilePart>()
        var originalName: String? = null
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
                        originalName = part.originalFileName
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
            if (originalName == null) originalName = call.request.queryParameters["originalName"]

            if (fileId != null && fileParts.size != 1) {
                throw BadRequestException("当fileId不为空或0时, 仅允许上传单个文件")
            }
            if (fileType == FileType.Assignment && fileParts.size != 1) {
                throw BadRequestException("上传作业时，仅允许上传单个文件")
            }

            val service: FileDecorator = fileType!!.decorator(call)
            // check owner
            if (!service.checkPermission(owner!!, involvedId!!)) {
                throw BadRequestException("无上传权限")
            }

            return ParseFormDataResult(
                fileParts,
                originalName!!,
                owner!!,
                fileType!!,
                involvedId!!,
                fileId,
                service,
            )

        } catch (e: Exception) {
            throw BadRequestException("文件上传参数错误，请修改后重试", e)
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
        file.fileType.decorator(call).manager().deleteFile(file.storeName)
    }

    suspend fun fetchProducer(file: File): suspend OutputStream.() -> Unit {
        call.user().assertRead(file)
        return { file.inputStreamSuspend().use { it.copyTo(this) } }
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

            FileType.ImageBuildContextTar ->
                call.user().assertWrite(Project.id(involvedId.toLong()))

            FileType.ExperimentWorkflowContext ->
                call.user().assertWrite(Experiment.id(involvedId))
        }
        // get files
        val service = fileType.decorator(call)
        val (files, readme, zipFilename) = service.packageFiles(involvedId, fileIdList)
        val packageId = "${UUID.randomUUID()}.package.tmp"
        packageResult[packageId] = false
        call.respond(FilePackageResponse(packageId, zipFilename))
        withContext(Dispatchers.IO) {
            val zipFile = java.io.File(packageId)
            zipFile.createNewFile()

            val threadFactory = ThreadFactoryBuilder().setNameFormat("package-%d").build()
            val executor = Executors.newFixedThreadPool(6, threadFactory)
            val parallelScatterZipCreator = ParallelScatterZipCreator(executor)
            val addEntry: (String, Long, () -> InputStream) -> Unit = { filename, size, inputStreamProvider ->
                val entry = ZipArchiveEntry(filename)
                entry.method = ZipArchiveEntry.DEFLATED
                entry.size = size
                entry.unixMode = UnixStat.FILE_FLAG or 436
                parallelScatterZipCreator.addArchiveEntry(entry, inputStreamProvider)
            }
            ZipArchiveOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                zipOut.encoding = "UTF-8"
                files.forEach { addEntry(it.name, it.size) { it.inputStream() } }
                addEntry("README", readme.length.toLong()) { readme.byteInputStream() }
                parallelScatterZipCreator.writeTo(zipOut)
            }
            executor.shutdownNow()
            packageResult[packageId] = true
        }
    }

    suspend fun createS3Uploader(): S3Uploader {
        val parseFormDataResult = parseFormData()
        val (_, storedName) = parseFormDataResult.fixName()
        return parseFormDataResult.decorator.s3UploaderAliasAndUserGroup(storedName)
    }
}

private fun File.getManager(): FileManager {
    return FileManager.buildFileManager(this.storeType, this.storePath)
}

fun File.inputStream(): InputStream {
    return this.getManager().inputStream(this.storeName)
}

suspend fun File.inputStreamSuspend(): InputStream {
    return this.getManager().inputStreamSuspend(this.storeName)
}

fun File.Companion.id(id: Int): File {
    return mysql.files.find { it.id eq id }
        ?: throw BusinessException("find file($id) from database error")
}
