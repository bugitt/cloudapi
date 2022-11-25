package cn.edu.buaa.scs.storage

import cn.edu.buaa.scs.utils.getConfigString
import io.ktor.server.application.*
import org.ktorm.jackson.KtormModule
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.util.KMongoConfiguration

lateinit var mClient: CoroutineClient
lateinit var mongo: CoroutineDatabase

@Suppress("unused")
fun Application.mongoModule() {
    val connectionString = getConfigString("db.mongo.connectionString")
    mClient = KMongo.createClient(connectionString).coroutine
    mongo = mClient.getDatabase(getConfigString("db.mongo.database"))
    KMongoConfiguration.registerBsonModule(KtormModule())
}