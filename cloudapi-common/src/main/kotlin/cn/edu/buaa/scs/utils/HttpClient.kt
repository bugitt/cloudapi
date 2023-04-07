package cn.edu.buaa.scs.utils

import cn.edu.buaa.scs.error.RemoteServiceException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class HttpClientWrapper(val client: HttpClient, val basePath: String) {
    suspend inline fun <reified T> handleResponse(response: HttpResponse): Result<T> =
        if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            Result.failure(RemoteServiceException(response.status.value, response.body()))
        }

    suspend inline fun <reified T> get(path: String): Result<T> {
        val response = client.get(basePath + path)
        return handleResponse(response)
    }

    suspend inline fun <reified T> post(
        path: String,
        body: Any? = null,
        contentType: ContentType = ContentType.Application.Json,
    ): Result<T> {
        val response = client.post(basePath + path) {
            contentType(contentType)
            setBody(body)
        }
        return handleResponse(response)
    }

    suspend inline fun <reified T> put(
        path: String,
        body: Any? = null,
        contentType: ContentType = ContentType.Application.Json,
    ): Result<T> {
        val response = client.put(basePath + path) {
            contentType(contentType)
            setBody(body)
        }
        return handleResponse(response)
    }

    suspend inline fun <reified T> delete(
        path: String,
        body: Any? = null,
        contentType: ContentType = ContentType.Application.Json,
    ): Result<T> {
        val response = client.delete(basePath + path) {
            contentType(contentType)
            setBody(body)
        }
        return handleResponse(response)
    }
}
