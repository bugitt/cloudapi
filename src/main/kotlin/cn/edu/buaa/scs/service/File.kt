package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.File
import cn.edu.buaa.scs.model.FileType
import cn.edu.buaa.scs.model.StoreType
import cn.edu.buaa.scs.storage.getFile
import cn.edu.buaa.scs.storage.uploadFile
import cn.edu.buaa.scs.utils.user
import cn.edu.buaa.scs.utils.userId
import io.ktor.application.*
import java.io.InputStream
import java.io.OutputStream
import java.util.*

val ApplicationCall.file: FileService
    get() = FileService(this)

class FileService(val call: ApplicationCall) {
    fun fetchProducer(file: File): suspend OutputStream.() -> Unit {
        call.user().assertRead(file)
        val inputStream = getFile(file.storePath, file.storeName)
        return { inputStream.copyTo(this) }
    }

    fun create(
        owner: String,
        originalName: String,
        fileType: FileType,
        inputStream: InputStream
    ): File {
        val fileResp = File.upload(fileType, inputStream)
        return File {
            name = originalName
            storeType = StoreType.S3
            storeName = fileResp.storeName
            storePath = Assignment.bucket
            uploadTime = fileResp.uploadTime
            this.fileType = fileType
            size = fileResp.size
            this.uploader = uploader
            this.owner = owner
            this.createdAt = System.currentTimeMillis()
            this.updatedAt = System.currentTimeMillis()
        }
    }

    fun update(file: File, inputStream: InputStream): File {
        File.upload(file.fileType, inputStream).let {
            file.storeName = it.storeName
            file.uploadTime = it.uploadTime
            file.size = it.size
            file.uploader = call.userId()
            file.updatedAt = System.currentTimeMillis()

            file.flushChanges()
        }
        return file
    }
}

fun File.Companion.upload(
    fileType: FileType,
    inputStream: InputStream
): FileResp {
    val storeFileName = UUID.randomUUID().toString()
    return when (fileType) {
        FileType.Assignment -> uploadFile(Assignment.bucket, storeFileName, inputStream)
    }.let {
        FileResp(storeFileName, it.size(), it.lastModified().toInstant().toEpochMilli())
    }
}

data class FileResp(
    val storeName: String,
    val size: Long,
    val uploadTime: Long,
)