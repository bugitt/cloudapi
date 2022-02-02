package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.getFile
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.storage.uploadFile
import cn.edu.buaa.scs.utils.updateFileExtension
import cn.edu.buaa.scs.utils.user
import cn.edu.buaa.scs.utils.userId
import io.ktor.application.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.tika.Tika
import org.ktorm.dsl.eq
import org.ktorm.entity.find
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*

val ApplicationCall.file: FileService
    get() = FileService(this)

class FileService(val call: ApplicationCall) {
    fun get(fileId: Int): File {
        val file = mysql.files.find { it.id eq fileId }
            ?: throw NotFoundException("assignment($fileId) not found")
        call.user().assertRead(file)
        return file
    }

    suspend fun createOrUpdate(
        readChannel: ByteReadChannel,
        originalName: String,
        owner: String,
        fileType: FileType,
        involvedId: Int,
        contentType: String,
        fileId: Int?
    ): File {
        val tmpFilename = UUID.randomUUID().toString()
        withContext(Dispatchers.IO) {
            FileOutputStream(java.io.File(tmpFilename)).use { out ->
                val byteBufferSize = 1024 * 100
                val byteBuffer = ByteArray(byteBufferSize)
                do {
                    val currentRead = readChannel.readAvailable(byteBuffer, 0, byteBufferSize)
                    if (currentRead > 0) {
                        out.write(byteBuffer)
                    }
                } while (currentRead > 0)
            }
        }
    }

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

fun File.Companion.id(id: Int): File {
    return mysql.files.find { it.id eq id }
        ?: throw BusinessException("find file($id) from database error")
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