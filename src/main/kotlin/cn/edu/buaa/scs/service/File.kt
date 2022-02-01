package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.File
import cn.edu.buaa.scs.model.FileType
import cn.edu.buaa.scs.model.StoreType
import cn.edu.buaa.scs.storage.getFile
import cn.edu.buaa.scs.storage.uploadFile
import cn.edu.buaa.scs.utils.updateFileExtension
import cn.edu.buaa.scs.utils.user
import cn.edu.buaa.scs.utils.userId
import io.ktor.application.*
import org.apache.tika.Tika
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*

val ApplicationCall.file: FileService
    get() = FileService(this)

class FileService(val call: ApplicationCall) {
    fun fetchProducer(file: File): suspend OutputStream.() -> Unit {
        call.user().assertRead(file)
        val inputStream = getFile(file.storePath, file.storeName)
        return { inputStream.use { it.copyTo(this) } }
    }

    fun create(
        owner: String,
        originalName: String,
        fileType: FileType,
        inputStream: InputStream
    ): File {
        val fileResp = File.upload(originalName, fileType, inputStream)
        return File {
            name = originalName
            storeType = StoreType.S3
            storeName = fileResp.storeName
            storePath = Assignment.bucket
            uploadTime = fileResp.uploadTime
            this.contentType = fileResp.contentType
            this.fileType = fileType
            size = fileResp.size
            this.uploader = call.userId()
            this.owner = owner
            this.createdAt = System.currentTimeMillis()
            this.updatedAt = System.currentTimeMillis()
        }
    }

    fun update(file: File, newFileName: String?, inputStream: InputStream): File {
        file.name = file.name.updateFileExtension(newFileName)
        File.upload(file.name, file.fileType, inputStream).let {
            file.storeName = it.storeName
            file.uploadTime = it.uploadTime
            file.size = it.size
            file.contentType = it.contentType
            file.uploader = call.userId()
            file.updatedAt = System.currentTimeMillis()

            file.flushChanges()
        }
        return file
        
    }
}

fun File.Companion.upload(
    originalName: String,
    fileType: FileType,
    inputStream: InputStream
): FileResp {
    // detect file type
    // TODO: large file
    val bytes = ByteArrayOutputStream().use { output ->
        inputStream.copyTo(output)
        output.toByteArray()
    }
    val contentType = bytes.inputStream().use { input ->
        Tika().detect(input, originalName)
    }

    val storeFileName = UUID.randomUUID().toString()
    // 上传文件
    return bytes.inputStream().use { input ->
        when (fileType) {
            FileType.Assignment -> uploadFile(Assignment.bucket, storeFileName, input, contentType, bytes.size.toLong())
        }
    }.let {
        FileResp(
            storeFileName,
            it.size(),
            it.contentType(),
            it.lastModified().toInstant().toEpochMilli()
        )
    }
}

data class FileResp(
    val storeName: String,
    val size: Long,
    val contentType: String,
    val uploadTime: Long,
)