package cn.edu.buaa.scs.storage

import io.ktor.server.plugins.*
import org.bson.types.ObjectId
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.CoroutineCollection

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
