package cn.edu.buaa.scs.storage.file

import java.io.InputStream

data class FileResp(
    val storeName: String,
    val size: Long,
    val contentType: String,
    val uploadTime: Long,
)

abstract class FileManager(protected val storePath: String) {

    companion object {
        fun buildFileManager(type: String, storePath: String): FileManager {
            return when (type.lowercase()) {
                "local" -> LocalFileManager(storePath)
                "s3" -> S3(storePath)
                else -> throw IllegalArgumentException("Unknown file manager type: $type")
            }
        }
    }

    abstract suspend fun name(): String

    abstract suspend fun uploadFile(
        storeName: String,
        inputStream: InputStream,
        contentType: String = "application/octet-stream",
        size: Long = -1L
    ): FileResp

    abstract suspend fun deleteFile(filename: String)

    abstract suspend fun getFile(filename: String): InputStream

    abstract suspend fun downloadFile(filename: String, targetFileName: String)
}