package cn.edu.buaa.scs.storage.file

import cn.edu.buaa.scs.config.globalConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.tika.Tika
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths

class LocalFileManager(basePath: String) :
    FileManager(Paths.get(globalConfig.storage.localSubPath, basePath).toString()) {
    val filePath: (storeName: String) -> String = { "$storePath/$it" }

    init {
        Files.createDirectories(Paths.get(storePath))
    }

    override suspend fun name(): String = "LOCAL"

    override suspend fun uploadFile(
        storeName: String,
        inputStream: InputStream,
        contentType: String,
        size: Long
    ): FileResp = withContext(Dispatchers.IO) {
        val path = Paths.get(filePath(storeName)).also {
            Files.createDirectories(it.parent)
        }
        val javaFile = path.toFile()
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
        Files.delete(Paths.get(filePath(filename)))
    }

    override suspend fun inputStreamSuspend(filename: String): InputStream = withContext(Dispatchers.IO) {
        java.io.File(filePath(filename)).inputStream()
    }

    override fun inputStream(filename: String): InputStream {
        return java.io.File(filePath(filename)).inputStream()
    }

    override suspend fun downloadFile(filename: String, targetFileName: String) = withContext(Dispatchers.IO) {
        Files.copy(
            Paths.get(filePath(filename)),
            Paths.get(targetFileName),
        )
        Unit
    }
}
