package cn.edu.buaa.scs.storage.file

import java.io.InputStream

data class FileResp(
    val storeName: String,
    val size: Long,
    val contentType: String,
    val uploadTime: Long,
)

interface FileManager {
    suspend fun uploadFile(
        storeName: String,
        inputStream: InputStream,
        contentType: String = "application/octet-stream",
        size: Long = -1L
    ): FileResp

    suspend fun deleteFile(filename: String)

    suspend fun getFile(filename: String): InputStream

    suspend fun downloadFile(filename: String, targetFileName: String)
}