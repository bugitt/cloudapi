package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertRead
import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.File
import cn.edu.buaa.scs.model.FileType
import cn.edu.buaa.scs.model.StoreType
import cn.edu.buaa.scs.storage.getFile
import cn.edu.buaa.scs.storage.uploadFile
import cn.edu.buaa.scs.utils.user
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
}

fun File.Companion.upload(
    owner: String,
    uploader: String,
    originalName: String,
    inputStream: InputStream
): File {
    val storeFileName = UUID.randomUUID().toString()
    val fileResp = uploadFile(Assignment.bucket, storeFileName, inputStream)
    return File {
        name = originalName
        storeType = StoreType.S3
        storeName = storeFileName
        storePath = Assignment.bucket
        uploadTime = fileResp.lastModified().toInstant().toEpochMilli()
        fileType = FileType.Assignment
        size = fileResp.size()
        this.uploader = uploader
        this.owner = owner
        this.createdAt = System.currentTimeMillis()
        this.updatedAt = System.currentTimeMillis()
    }
}