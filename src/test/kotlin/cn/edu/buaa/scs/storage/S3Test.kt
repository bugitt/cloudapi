package cn.edu.buaa.scs.storage

import cn.edu.buaa.scs.storage.file.S3
import cn.edu.buaa.scs.storage.file.minioClient
import cn.edu.buaa.scs.testEnv
import io.ktor.server.testing.*
import io.minio.BucketExistsArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class S3TEst {
    @Test
    fun testS3ModuleInit() {
        withApplication(testEnv) {
            assert(
                minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket("scsos").build()
                )
            )
        }
    }

    @Test
    fun testFileManagement() {
        withApplication(testEnv) {
            val content = "12345"
            val bucket = "tmp"
            val filename = UUID.randomUUID().toString()
            val fullFilePath = "${getBaseFilePath()}/$filename"

            val s3Uploader = S3(bucket)

            runBlocking {
                withContext(Dispatchers.IO) {
                    // upload
                    val fileInfo = fullFilePath
                        .let { File(it).apply { writeText(content) } }
                        .let { FileInputStream(it) }
                        .use { s3Uploader.uploadFile(filename, it) }

                    assert(fileInfo.size == 5L)

                    // get
                    assert(String(s3Uploader.getFile(filename).readBytes()) == content)

                    // download
                    val newFilepath = "${getBaseFilePath()}/$filename-2"
                    s3Uploader.downloadFile(filename, newFilepath)
                    assert(Files.exists(Paths.get(newFilepath)))

                    // delete
                    s3Uploader.deleteFile(filename)

                    // delete local file
                    Files.delete(Paths.get(fullFilePath))
                    Files.delete(Paths.get(newFilepath))
                }
            }
        }

    }

    private fun getBaseFilePath() = Paths.get(".").toAbsolutePath().toString()
}