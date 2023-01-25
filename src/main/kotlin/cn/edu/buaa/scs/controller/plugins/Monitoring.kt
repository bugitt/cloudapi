package cn.edu.buaa.scs.controller.plugins

import cn.edu.buaa.scs.model.LogRecord
import cn.edu.buaa.scs.model.LogRecordReq
import cn.edu.buaa.scs.model.LogRecordResp
import cn.edu.buaa.scs.model.logRecord
import cn.edu.buaa.scs.storage.mongo
import cn.edu.buaa.scs.utils.getError
import cn.edu.buaa.scs.utils.userOrNull
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    val startTimestampAttrKey = AttributeKey<Long>("start-time-stamp")
    val logRecordKey = AttributeKey<LogRecord>("log-record")

    fun logRecord(call: ApplicationCall): LogRecord {
        return call.attributes.getOrNull(logRecordKey) ?: run {
            val reqID = call.response.headers[HttpHeaders.XRequestId]
            val method = call.request.httpMethod.value
            val requestUrl = call.request.path()
            val version = call.request.httpVersion
            val status = call.response.status()
            val userAgent = call.request.userAgent()

            val logRecordReq = LogRecordReq(
                method = method,
                path = requestUrl,
                pathDescription = "",
                headers = call.request.headers.toMap(),
                version = version,
                realIp = call.request.headers["X-Custom-Remote-Addr"] ?: call.request.origin.host,
                userAgent = userAgent,
            )
            val logRecordResp = LogRecordResp(
                status = status?.value,
                errMsg = call.getError()?.stackTraceToString(),
                headers = call.response.headers.allValues().toMap(),
            )
            val startAt = call.attributes.getOrNull(startTimestampAttrKey)
            LogRecord(
                requestId = reqID,
                user = call.userOrNull(),
                request = logRecordReq,
                response = logRecordResp,
                startAt = startAt,
                duration = startAt?.let {
                    System.currentTimeMillis() - it
                },
            )
        }.also {
            call.attributes.put(logRecordKey, it)
        }
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
        format { call ->
            val logRecord = logRecord(call)
            """${logRecord.requestId} - ${logRecord.user?.id} - ${logRecord.request.method} ${logRecord.request.path} ${logRecord.response.status} ${logRecord.request.userAgent}"""
        }
    }

    install(createApplicationPlugin("storeLogRecord") {
        on(CallSetup) { call ->
            call.attributes.put(startTimestampAttrKey, System.currentTimeMillis())
        }

        on(ResponseSentSuspend) { call ->
            val logRecord = logRecord(call)
            if (logRecord.request.realIp?.contains("127.0.0.1") != true) {
                mongo.logRecord.insertOne(logRecord)
            }
        }
    })
}

internal object ResponseSentSuspend : Hook<suspend (ApplicationCall) -> Unit> {
    override fun install(pipeline: ApplicationCallPipeline, handler: suspend (ApplicationCall) -> Unit) {
        pipeline.sendPipeline.intercept(ApplicationSendPipeline.Engine) {
            proceed()
            handler(call)
        }
    }
}
