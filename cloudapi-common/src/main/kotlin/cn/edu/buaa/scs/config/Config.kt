package cn.edu.buaa.scs.config

import cn.edu.buaa.scs.storage.mongo
import kotlinx.coroutines.runBlocking
import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.Id
import org.litote.kmongo.eq
import org.litote.kmongo.newId

val globalConfig by lazy {
    runBlocking {
        mongo.getCollection<Config>().find(Config::envType eq (System.getenv("ENV_TYPE") ?: "dev")).first()
            ?: throw IllegalStateException("No such config for ${System.getenv("ENV_TYPE") ?: "dev"}")
    }
}

data class Config(
    @BsonId val _id: Id<Config> = newId(),
    val envType: String,
    val vcenter: VCenter,
) {
    data class VCenter(
        val port: Int,
        val entrypoint: String,
        val username: String,
        val password: String,
        val serviceUrl: String,
        val serviceToken: String,
    )
}
