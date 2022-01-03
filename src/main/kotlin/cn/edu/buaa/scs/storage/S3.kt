package cn.edu.buaa.scs.storage

import cn.edu.buaa.scs.getConfigString
import cn.edu.buaa.scs.utils.logger
import io.ktor.application.*
import io.minio.BucketExistsArgs
import io.minio.MinioClient

lateinit var minioClient: MinioClient


@Suppress("unused")
fun Application.s3Module() {
    val endPoint = getConfigString("s3.endPoint")
    val accessKey = getConfigString("s3.accessKey")
    val secretKey = getConfigString("s3.secretKey")
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