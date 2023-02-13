package cn.edu.buaa.scs.storage

import cn.edu.buaa.scs.utils.getConfigString
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import org.bson.types.ObjectId
import org.ktorm.jackson.KtormModule
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
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

interface MongoIdFinder<T : Any> {
    val col: CoroutineCollection<T>

    suspend fun id(id: String): T {
        return col.findOneById(ObjectId(id))
            ?: throw NotFoundException("No such ${this::class.simpleName}($id) in database")
    }

    suspend fun id(id: Id<T>): T {
        return col.findOneById(id)
            ?: throw NotFoundException("No such ${this::class.simpleName}($id) in database")
    }

    suspend fun id(id: ObjectId): T {
        return col.findOneById(id)
            ?: throw NotFoundException("No such ${this::class.simpleName}($id) in database")
    }
}
