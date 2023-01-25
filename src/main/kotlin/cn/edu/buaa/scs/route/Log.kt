package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.model.LogRecord
import cn.edu.buaa.scs.service.log
import cn.edu.buaa.scs.utils.formatHeaders
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.logRoute() {
    route("/logs") {
        post {
            call.respond(
                call.log.search(call.receive()).map { convertLogRecordResponse(it) }
            )
        }
    }
}

internal fun convertLogRecordResponse(record: LogRecord): cn.edu.buaa.scs.controller.models.LogRecord {
    val user = record.user
    val req = record.request
    val response = record.response
    return cn.edu.buaa.scs.controller.models.LogRecord(
        id = record._id.toString(),
        userId = user?.id,
        username = user?.name,
        method = req.method.uppercase(),
        path = req.path,
        requestHeaders = formatHeaders(req.headers),
        httpVersion = req.version,
        realIp = req.realIp,
        userAgent = req.userAgent,
        responseStatus = response.status,
        responseHeaders = formatHeaders(response.headers),
        errMsg = response.errMsg,
        startAt = record.startAt,
        duration = record.duration,
    )
}
