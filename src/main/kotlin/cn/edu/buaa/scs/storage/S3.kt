package cn.edu.buaa.scs.storage

import cn.edu.buaa.scs.utils.getConfigString
import cn.edu.buaa.scs.utils.logger
import io.ktor.application.*
import io.minio.*
import java.io.InputStream


lateinit var minioClient: MinioClient
var minioPartSize: Long = 10485760L

fun uploadFile(
    bucket: String,
    filename: String,
    inputStream: InputStream,
    contentType: String = "application/octet-stream",
    size: Long = -1L
): StatObjectResponse {
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
    return inspectFile(bucket, filename)
}

fun inspectFile(bucket: String, filename: String): StatObjectResponse =
    StatObjectArgs
        .builder()
        .bucket(bucket)
        .`object`(filename)
        .build()
        .let { minioClient.statObject(it) }


fun deleteFile(bucket: String, filename: String) {
    RemoveObjectArgs
        .builder()
        .bucket(bucket)
        .`object`(filename)
        .build()
        .let { minioClient.removeObject(it) }
}

fun getFile(bucket: String, filename: String): InputStream =
    GetObjectArgs
        .builder()
        .bucket(bucket)
        .`object`(filename)
        .build()
        .let { minioClient.getObject(it) }

fun downloadFile(bucket: String, filename: String, targetFileName: String) {
    DownloadObjectArgs
        .builder()
        .bucket(bucket)
        .`object`(filename)
        .filename(targetFileName)
        .build()
        .let { minioClient.downloadObject(it) }
}


fun ensureBucketExists(bucket: String) {
    minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()).let {
        if (!it) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
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