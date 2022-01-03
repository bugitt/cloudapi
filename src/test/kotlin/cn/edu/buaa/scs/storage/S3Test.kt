package cn.edu.buaa.scs.storage

import cn.edu.buaa.scs.testEnv
import io.ktor.server.testing.*
import io.minio.BucketExistsArgs
import org.junit.Test

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
}