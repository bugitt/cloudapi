package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.controller.models.LogRecordSearchRequest
import cn.edu.buaa.scs.error.AuthenticationException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.storage.mongo
import cn.edu.buaa.scs.utils.user
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import io.ktor.server.application.*
import org.bson.conversions.Bson
import org.litote.kmongo.*


val ApplicationCall.log
    get() = LogService.getSvc(this) { LogService(this) }

class LogService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<LogService>()

    suspend fun search(req: LogRecordSearchRequest): List<LogRecord> {
        if (!call.user().isAdmin()) {
            throw AuthenticationException()
        }

        val skip = (req.pagination.current - 1) * req.pagination.pageSize
        val limit = req.pagination.pageSize

        val filters = mutableListOf<Bson>()
        if (req.userId != null) {
            filters.add(LogRecord::user / User::id regex "*${req.userId}*")
        }
        if (req.username != null) {
            filters.add(LogRecord::user / User::name regex "*${req.username}*")
        }
        if (req.method != null) {
            filters.add(LogRecord::request / LogRecordReq::method eq req.method.uppercase())
        }
        if (req.path != null) {
            filters.add(LogRecord::request / LogRecordReq::path regex "*${req.path}*")
        }
        if (req.status != null) {
            filters.add(LogRecord::response / LogRecordResp::status eq req.status)
        }
        if (req.errMsg != null) {
            filters.add(LogRecord::response / LogRecordResp::errMsg regex "*${req.errMsg}*")
        }
        if (req.timeRange != null) {
            val (start, end) = req.timeRange.split("-").let { it[0].toLong() to it[1].toLong() }
            filters.add(LogRecord::startAt gte start)
            filters.add(LogRecord::startAt lte end)
        }

        val sortByList = mutableListOf<Bson>()
        if (req.order != null) {
            if (req.order.startAt != null) {
                if (req.order.startAt.value.lowercase().startsWith("asc")) {
                    sortByList.add(Sorts.ascending(LogRecord::startAt.name))
                } else {
                    sortByList.add(Sorts.descending(LogRecord::startAt.name))
                }
            }
            if (req.order.duration != null) {
                if (req.order.duration.value.lowercase().startsWith("asc")) {
                    sortByList.add(Sorts.ascending(LogRecord::duration.name))
                } else {
                    sortByList.add(Sorts.descending(LogRecord::duration.name))
                }
            }
        }

        return mongo.logRecord
            .find(Filters.and(filters))
            .sort(Sorts.orderBy(sortByList))
            .skip(skip)
            .limit(limit)
            .toList()
    }
}
