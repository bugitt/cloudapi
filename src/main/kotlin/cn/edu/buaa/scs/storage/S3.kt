package cn.edu.buaa.scs.storage

import cn.edu.buaa.scs.utils.getConfigString
import cn.edu.buaa.scs.utils.logger
import io.ktor.application.*
import io.minio.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream


lateinit var minioClient: MinioClient
var minioPartSize: Long = 10485760L

class S3(private val bucket: String) {

    companion object {
        private val dispatcher: CoroutineDispatcher = Dispatchers.IO
    }

    data class FileResp(
        val storeName: String,
        val size: Long,
        val contentType: String,
        val uploadTime: Long,
    )

    suspend fun uploadFile(
        filename: String,
        inputStream: InputStream,
        contentType: String = "application/octet-stream",
        size: Long = -1L
    ): FileResp = withContext(dispatcher) {
        ensureBucketExists(bucket)
        PutObjectArgs
            .builder()
            .bucket(bucket)
            .`object`(filename)
            .also {
                if (size == -1L) it.stream(inputStream, -1, minioPartSize)
                else it.stream(inputStream, size, -1)
            }
            .contentType(contentType)
            .build()
            .let { minioClient.putObject(it) }
        inspectFile(filename).let {
            FileResp(filename, it.size(), it.contentType(), it.lastModified().toInstant().toEpochMilli())
        }
    }

    private suspend fun inspectFile(filename: String): StatObjectResponse = withContext(dispatcher) {
        StatObjectArgs
            .builder()
            .bucket(bucket)
            .`object`(filename)
            .build()
            .let { minioClient.statObject(it) }
    }


    suspend fun deleteFile(filename: String) = withContext(dispatcher) {
        RemoveObjectArgs
            .builder()
            .bucket(bucket)
            .`object`(filename)
            .build()
            .let { minioClient.removeObject(it) }
    }

    suspend fun getFile(filename: String): InputStream = withContext(dispatcher) {
        GetObjectArgs
            .builder()
            .bucket(bucket)
            .`object`(filename)
            .build()
            .let { minioClient.getObject(it) }
    }

    suspend fun downloadFile(filename: String, targetFileName: String) = withContext(dispatcher) {
        DownloadObjectArgs
            .builder()
            .bucket(bucket)
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