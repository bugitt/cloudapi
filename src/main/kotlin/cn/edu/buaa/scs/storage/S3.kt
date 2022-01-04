package cn.edu.buaa.scs.storage

import cn.edu.buaa.scs.getConfigString
import cn.edu.buaa.scs.utils.logger
import cn.edu.buaa.scs.utils.value
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.OctetStream
import io.minio.*
import java.io.InputStream


lateinit var minioClient: MinioClient
var minioPartSize: Long = 10485760L

fun uploadFile(
    bucket: String,
    filename: String,
    inputStream: InputStream,
    contentType: ContentType = OctetStream
) {
    ensureBucketExists(bucket)
    PutObjectArgs
        .builder()
        .bucket(bucket)
        .`object`(filename)
        .stream(inputStream, -1, minioPartSize)
        .contentType(contentType.value)
        .build()
        .let { minioClient.putObject(it) }

}

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
    println(endPoint)
    println(accessKey)
    println(secretKey)
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