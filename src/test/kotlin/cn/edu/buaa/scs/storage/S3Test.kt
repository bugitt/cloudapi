package cn.edu.buaa.scs.storage

import cn.edu.buaa.scs.testEnv
import io.ktor.server.testing.*
import io.minio.BucketExistsArgs
import org.junit.Test
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
    fun testEnsureBucketExists() {
        withApplication(testEnv) {
            ensureBucketExists("scsos3")
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

            // upload
            fullFilePath
                .let { File(it).apply { writeText(content) } }
                .let { FileInputStream(it) }
                .use { uploadFile(bucket, filename, it) }

            // get
            assert(String(getFile(bucket, filename).readAllBytes()) == content)

            // download
            val newFilepath = "${getBaseFilePath()}/$filename-2"
            downloadFile(bucket, filename, newFilepath)
            assert(Files.exists(Paths.get(newFilepath)))

            // delete
            deleteFile(bucket, filename)

            // delete local file
            Files.delete(Paths.get(fullFilePath))
            Files.delete(Paths.get(newFilepath))
        }

    }

    private fun getBaseFilePath() = Paths.get(".").toAbsolutePath().toString()
}