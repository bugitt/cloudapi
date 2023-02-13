@file:Suppress("unused")

package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.storage.MongoIdFinder
import cn.edu.buaa.scs.storage.mongo
import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.Id
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.newId

data class LogRecord(
    @BsonId val _id: Id<LogRecord> = newId(),
    val requestId: String?,
    val user: User?,
    val request: LogRecordReq,
    val response: LogRecordResp,
    val startAt: Long?, // timestamp
    val duration: Long?,  // ms
) {
    companion object : MongoIdFinder<LogRecord> {
        override val col: CoroutineCollection<LogRecord>
            get() = mongo.getCollection()
    }
}

data class LogRecordReq(
    val method: String,
    val path: String,
    val pathDescription: String,
    val headers: Map<String, List<String>>,
    val version: String,
    val realIp: String?,
    val userAgent: String?,
)

data class LogRecordResp(
    val status: Int?,
    val errMsg: String?,
    val headers: Map<String, List<String>>,
)

val CoroutineDatabase.logRecord get() = getCollection<LogRecord>()
