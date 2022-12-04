package cn.edu.buaa.scs.storage.file

import cn.edu.buaa.scs.utils.getConfigString
import cn.edu.buaa.scs.utils.logger
import io.ktor.server.application.*
import io.minio.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream


lateinit var minioClient: MinioClient
var minioPartSize: Long = 10485760L

class S3(storePath: String) : FileManager(storePath) {

    companion object {
        private val dispatcher: CoroutineDispatcher = Dispatchers.IO
    }

    override suspend fun name(): String = "S3"

    override suspend fun uploadFile(
        storeName: String,
        inputStream: InputStream,
        contentType: String,
        size: Long
    ): FileResp = withContext(dispatcher) {
        ensureBucketExists(storePath)
        PutObjectArgs
            .builder()
            .bucket(storePath)
            .`object`(storeName)
            .apply {
                if (size == -1L) this.stream(inputStream, -1, minioPartSize)
                else this.stream(inputStream, size, -1)
            }
            .contentType(contentType)
            .build()
            .let { minioClient.putObject(it) }
        inspectFile(storeName).let {
            FileResp(storeName, it.size(), it.contentType(), it.lastModified().toInstant().toEpochMilli())
        }
    }

    private suspend fun inspectFile(filename: String): StatObjectResponse = withContext(dispatcher) {
        StatObjectArgs
            .builder()
            .bucket(storePath)
            .`object`(filename)
            .build()
            .let { minioClient.statObject(it) }
    }


    override suspend fun deleteFile(filename: String) = withContext(dispatcher) {
        RemoveObjectArgs
            .builder()
            .bucket(storePath)
            .`object`(filename)
            .build()
            .let { minioClient.removeObject(it) }
    }

    override suspend fun inputStreamSuspend(filename: String): InputStream = withContext(dispatcher) {
        inputStream(filename)
    }

    override fun inputStream(filename: String): InputStream {
        return GetObjectArgs
            .builder()
            .bucket(storePath)
            .`object`(filename)
            .build()
            .let { minioClient.getObject(it) }
    }

    override suspend fun downloadFile(filename: String, targetFileName: String) = withContext(dispatcher) {
        DownloadObjectArgs
            .builder()
            .bucket(storePath)
            .`object`(filename)
            .filename(targetFileName)
            .build()
            .let { minioClient.downloadObject(it) }
    }


    private suspend fun ensureBucketExists(bucket: String) = withContext(dispatcher) {
        minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()).let {
            if (!it) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
            }
        }
    }
}


@Suppress("unused")
fun Application.s3Module() {
    val endPoint = getConfigString("s3.endPoint")
    val accessKey = getConfigString("s3.accessKey")
    val secretKey = getConfigString("s3.secretKey")

    minioPartSize = getConfigString("s3.partSize").toLong()
    minioClient = MinioClient.builder()
        .endpoint(endPoint)
        .credentials(accessKey, secretKey)
        .build()

    logger("s3")().info {
        "connected to s3-server successfully: ${
            minioClient.bucketExists(
                BucketExistsArgs.builder().bucket("scsos").build()
            )
        }"
    }
}