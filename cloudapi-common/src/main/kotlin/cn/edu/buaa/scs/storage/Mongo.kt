package cn.edu.buaa.scs.storage

import org.ktorm.jackson.KtormModule
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.util.KMongoConfiguration

val mClient: CoroutineClient = run {
    val connectionString = System.getenv("MONGO_URL") ?: "mongodb://localhost:27017"
    val mClient = KMongo.createClient(connectionString).coroutine
    KMongoConfiguration.registerBsonModule(KtormModule())
    mClient
}

val mongo by lazy { mClient.getDatabase(System.getenv("MONGO_DB") ?: "cloud") }
