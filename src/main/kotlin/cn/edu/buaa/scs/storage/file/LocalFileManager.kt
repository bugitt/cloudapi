package cn.edu.buaa.scs.storage.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.tika.Tika
import java.io.InputStream

class LocalFileManager(basePath: String) : FileManager(basePath) {
    val filePath: (storeName: String) -> String = { "$storePath/$it" }

    override suspend fun name(): String = "LOCAL"

    override suspend fun uploadFile(
        storeName: String,
        inputStream: InputStream,
        contentType: String,
        size: Long
    ): FileResp = withContext(Dispatchers.IO) {
        val javaFile = java.io.File(filePath(storeName))
        javaFile.createNewFile()
        javaFile.outputStream().use {
            inputStream.copyTo(it)
        }
        FileResp(
            storeName,
            javaFile.length(),
            if (contentType == "application/octet-stream") Tika().detect(javaFile) else contentType,
            System.currentTimeMillis()
        )
    }

    override suspend fun deleteFile(filename: String) = withContext(Dispatchers.IO) {
        java.nio.file.Files.delete(java.nio.file.Paths.get(filePath(filename)))
    }

    override suspend fun getFile(filename: String): InputStream {
        return java.io.File(filePath(filename)).inputStream()
    }

    override suspend fun downloadFile(filename: String, targetFileName: String) = withContext(Dispatchers.IO) {
        java.nio.file.Files.copy(
            java.nio.file.Paths.get(filePath(filename)),
            java.nio.file.Paths.get(targetFileName),
        )
        Unit
    }
}