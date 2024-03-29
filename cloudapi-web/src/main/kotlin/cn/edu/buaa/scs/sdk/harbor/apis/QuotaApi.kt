/**
 * Harbor API
 *
 * These APIs provide services for manipulating Harbor project.
 *
 * The version of the OpenAPI document: 2.0
 *
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package cn.edu.buaa.scs.sdk.harbor.apis

import cn.edu.buaa.scs.sdk.harbor.infrastructure.*
import cn.edu.buaa.scs.sdk.harbor.models.Quota
import cn.edu.buaa.scs.sdk.harbor.models.QuotaUpdateReq
import okhttp3.OkHttpClient
import java.io.IOException

class QuotaApi(basePath: kotlin.String = defaultBasePath, client: OkHttpClient = ApiClient.defaultClient) :
    ApiClient(basePath, client) {
    companion object {
        @JvmStatic
        val defaultBasePath: String by lazy {
            System.getProperties().getProperty(ApiClient.baseUrlKey, "https://localhost/api/v2.0")
        }
    }

    /**
     * Get the specified quota
     * Get the specified quota
     * @param id Quota ID
     * @param xRequestId An unique ID for the request (optional)
     * @return Quota
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(
        IllegalStateException::class,
        IOException::class,
        UnsupportedOperationException::class,
        ClientException::class,
        ServerException::class
    )
    fun getQuota(id: kotlin.Int, xRequestId: kotlin.String? = null): Quota {
        val localVarResponse = getQuotaWithHttpInfo(id = id, xRequestId = xRequestId)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as Quota
            ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
            ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
            ResponseType.ClientError -> {
                val localVarError = localVarResponse as ClientError<*>
                throw ClientException(
                    "Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}",
                    localVarError.statusCode,
                    localVarResponse
                )
            }

            ResponseType.ServerError -> {
                val localVarError = localVarResponse as ServerError<*>
                throw ServerException(
                    "Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}",
                    localVarError.statusCode,
                    localVarResponse
                )
            }
        }
    }

    /**
     * Get the specified quota
     * Get the specified quota
     * @param id Quota ID
     * @param xRequestId An unique ID for the request (optional)
     * @return ApiResponse<Quota?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun getQuotaWithHttpInfo(id: kotlin.Int, xRequestId: kotlin.String?): ApiResponse<Quota?> {
        val localVariableConfig = getQuotaRequestConfig(id = id, xRequestId = xRequestId)

        return request<Unit, Quota>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getQuota
     *
     * @param id Quota ID
     * @param xRequestId An unique ID for the request (optional)
     * @return RequestConfig
     */
    fun getQuotaRequestConfig(id: kotlin.Int, xRequestId: kotlin.String?): RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/quotas/{id}".replace("{" + "id" + "}", id.toString()),
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

    /**
     * List quotas
     * List quotas
     * @param xRequestId An unique ID for the request (optional)
     * @param page The page number (optional, default to 1)
     * @param pageSize The size of per page (optional, default to 10)
     * @param reference The reference type of quota. (optional)
     * @param referenceId The reference id of quota. (optional)
     * @param sort Sort method, valid values include: &#39;hard.resource_name&#39;, &#39;-hard.resource_name&#39;, &#39;used.resource_name&#39;, &#39;-used.resource_name&#39;. Here &#39;-&#39; stands for descending order, resource_name should be the real resource name of the quota.  (optional)
     * @return kotlin.collections.List<Quota>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(
        IllegalStateException::class,
        IOException::class,
        UnsupportedOperationException::class,
        ClientException::class,
        ServerException::class
    )
    fun listQuotas(
        xRequestId: kotlin.String? = null,
        page: kotlin.Long? = 1,
        pageSize: kotlin.Long? = 10,
        reference: kotlin.String? = null,
        referenceId: kotlin.String? = null,
        sort: kotlin.String? = null
    ): kotlin.collections.List<Quota> {
        val localVarResponse = listQuotasWithHttpInfo(
            xRequestId = xRequestId,
            page = page,
            pageSize = pageSize,
            reference = reference,
            referenceId = referenceId,
            sort = sort
        )

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as kotlin.collections.List<Quota>
            ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
            ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
            ResponseType.ClientError -> {
                val localVarError = localVarResponse as ClientError<*>
                throw ClientException(
                    "Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}",
                    localVarError.statusCode,
                    localVarResponse
                )
            }

            ResponseType.ServerError -> {
                val localVarError = localVarResponse as ServerError<*>
                throw ServerException(
                    "Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}",
                    localVarError.statusCode,
                    localVarResponse
                )
            }
        }
    }

    /**
     * List quotas
     * List quotas
     * @param xRequestId An unique ID for the request (optional)
     * @param page The page number (optional, default to 1)
     * @param pageSize The size of per page (optional, default to 10)
     * @param reference The reference type of quota. (optional)
     * @param referenceId The reference id of quota. (optional)
     * @param sort Sort method, valid values include: &#39;hard.resource_name&#39;, &#39;-hard.resource_name&#39;, &#39;used.resource_name&#39;, &#39;-used.resource_name&#39;. Here &#39;-&#39; stands for descending order, resource_name should be the real resource name of the quota.  (optional)
     * @return ApiResponse<kotlin.collections.List<Quota>?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun listQuotasWithHttpInfo(
        xRequestId: kotlin.String?,
        page: kotlin.Long?,
        pageSize: kotlin.Long?,
        reference: kotlin.String?,
        referenceId: kotlin.String?,
        sort: kotlin.String?
    ): ApiResponse<kotlin.collections.List<Quota>?> {
        val localVariableConfig = listQuotasRequestConfig(
            xRequestId = xRequestId,
            page = page,
            pageSize = pageSize,
            reference = reference,
            referenceId = referenceId,
            sort = sort
        )

        return request<Unit, kotlin.collections.List<Quota>>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation listQuotas
     *
     * @param xRequestId An unique ID for the request (optional)
     * @param page The page number (optional, default to 1)
     * @param pageSize The size of per page (optional, default to 10)
     * @param reference The reference type of quota. (optional)
     * @param referenceId The reference id of quota. (optional)
     * @param sort Sort method, valid values include: &#39;hard.resource_name&#39;, &#39;-hard.resource_name&#39;, &#39;used.resource_name&#39;, &#39;-used.resource_name&#39;. Here &#39;-&#39; stands for descending order, resource_name should be the real resource name of the quota.  (optional)
     * @return RequestConfig
     */
    fun listQuotasRequestConfig(
        xRequestId: kotlin.String?,
        page: kotlin.Long?,
        pageSize: kotlin.Long?,
        reference: kotlin.String?,
        referenceId: kotlin.String?,
        sort: kotlin.String?
    ): RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf<kotlin.String, kotlin.collections.List<kotlin.String>>()
            .apply {
                if (page != null) {
                    put("page", listOf(page.toString()))
                }
                if (pageSize != null) {
                    put("page_size", listOf(pageSize.toString()))
                }
                if (reference != null) {
                    put("reference", listOf(reference.toString()))
                }
                if (referenceId != null) {
                    put("reference_id", listOf(referenceId.toString()))
                }
                if (sort != null) {
                    put("sort", listOf(sort.toString()))
                }
            }
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/quotas",
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

    /**
     * Update the specified quota
     * Update hard limits of the specified quota
     * @param id Quota ID
     * @param hard The new hard limits for the quota
     * @param xRequestId An unique ID for the request (optional)
     * @return void
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     * @throws UnsupportedOperationException If the API returns an informational or redirection response
     * @throws ClientException If the API returns a client error response
     * @throws ServerException If the API returns a server error response
     */
    @Throws(
        IllegalStateException::class,
        IOException::class,
        UnsupportedOperationException::class,
        ClientException::class,
        ServerException::class
    )
    fun updateQuota(id: kotlin.Int, hard: QuotaUpdateReq, xRequestId: kotlin.String? = null): Unit {
        val localVarResponse = updateQuotaWithHttpInfo(id = id, hard = hard, xRequestId = xRequestId)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> Unit
            ResponseType.Informational -> throw UnsupportedOperationException("Client does not support Informational responses.")
            ResponseType.Redirection -> throw UnsupportedOperationException("Client does not support Redirection responses.")
            ResponseType.ClientError -> {
                val localVarError = localVarResponse as ClientError<*>
                throw ClientException(
                    "Client error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}",
                    localVarError.statusCode,
                    localVarResponse
                )
            }

            ResponseType.ServerError -> {
                val localVarError = localVarResponse as ServerError<*>
                throw ServerException(
                    "Server error : ${localVarError.statusCode} ${localVarError.message.orEmpty()}",
                    localVarError.statusCode,
                    localVarResponse
                )
            }
        }
    }

    /**
     * Update the specified quota
     * Update hard limits of the specified quota
     * @param id Quota ID
     * @param hard The new hard limits for the quota
     * @param xRequestId An unique ID for the request (optional)
     * @return ApiResponse<Unit?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Throws(IllegalStateException::class, IOException::class)
    fun updateQuotaWithHttpInfo(id: kotlin.Int, hard: QuotaUpdateReq, xRequestId: kotlin.String?): ApiResponse<Unit?> {
        val localVariableConfig = updateQuotaRequestConfig(id = id, hard = hard, xRequestId = xRequestId)

        return request<QuotaUpdateReq, Unit>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation updateQuota
     *
     * @param id Quota ID
     * @param hard The new hard limits for the quota
     * @param xRequestId An unique ID for the request (optional)
     * @return RequestConfig
     */
    fun updateQuotaRequestConfig(
        id: kotlin.Int,
        hard: QuotaUpdateReq,
        xRequestId: kotlin.String?
    ): RequestConfig<QuotaUpdateReq> {
        val localVariableBody = hard
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }
        localVariableHeaders["Content-Type"] = "application/json"
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.PUT,
            path = "/quotas/{id}".replace("{" + "id" + "}", id.toString()),
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

}
