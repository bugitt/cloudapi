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
import cn.edu.buaa.scs.sdk.harbor.models.WebhookJob
import okhttp3.OkHttpClient
import java.io.IOException

class WebhookjobApi(basePath: kotlin.String = defaultBasePath, client: OkHttpClient = ApiClient.defaultClient) :
    ApiClient(basePath, client) {
    companion object {
        @JvmStatic
        val defaultBasePath: String by lazy {
            System.getProperties().getProperty(ApiClient.baseUrlKey, "https://localhost/api/v2.0")
        }
    }

    /**
     * List project webhook jobs
     * This endpoint returns webhook jobs of a project.
     * @param projectNameOrId The name or id of the project
     * @param policyId The policy ID.
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @param q Query string to query resources. Supported query patterns are \&quot;exact match(k&#x3D;v)\&quot;, \&quot;fuzzy match(k&#x3D;~v)\&quot;, \&quot;range(k&#x3D;[min~max])\&quot;, \&quot;list with union releationship(k&#x3D;{v1 v2 v3})\&quot; and \&quot;list with intersetion relationship(k&#x3D;(v1 v2 v3))\&quot;. The value of range and list can be string(enclosed by \&quot; or &#39;), integer or time(in format \&quot;2020-04-09 02:36:00\&quot;). All of these query patterns should be put in the query string \&quot;q&#x3D;xxx\&quot; and splitted by \&quot;,\&quot;. e.g. q&#x3D;k1&#x3D;v1,k2&#x3D;~v2,k3&#x3D;[min~max] (optional)
     * @param sort Sort the resource list in ascending or descending order. e.g. sort by field1 in ascending orderr and field2 in descending order with \&quot;sort&#x3D;field1,-field2\&quot; (optional)
     * @param page The page number (optional, default to 1)
     * @param pageSize The size of per page (optional, default to 10)
     * @param status The status of webhook job. (optional)
     * @return kotlin.collections.List<WebhookJob>
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
    fun listWebhookJobs(
        projectNameOrId: kotlin.String,
        policyId: kotlin.Long,
        xRequestId: kotlin.String? = null,
        xIsResourceName: kotlin.Boolean? = false,
        q: kotlin.String? = null,
        sort: kotlin.String? = null,
        page: kotlin.Long? = 1,
        pageSize: kotlin.Long? = 10,
        status: kotlin.collections.List<kotlin.String>? = null
    ): kotlin.collections.List<WebhookJob> {
        val localVarResponse = listWebhookJobsWithHttpInfo(
            projectNameOrId = projectNameOrId,
            policyId = policyId,
            xRequestId = xRequestId,
            xIsResourceName = xIsResourceName,
            q = q,
            sort = sort,
            page = page,
            pageSize = pageSize,
            status = status
        )

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as kotlin.collections.List<WebhookJob>
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
     * List project webhook jobs
     * This endpoint returns webhook jobs of a project.
     * @param projectNameOrId The name or id of the project
     * @param policyId The policy ID.
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @param q Query string to query resources. Supported query patterns are \&quot;exact match(k&#x3D;v)\&quot;, \&quot;fuzzy match(k&#x3D;~v)\&quot;, \&quot;range(k&#x3D;[min~max])\&quot;, \&quot;list with union releationship(k&#x3D;{v1 v2 v3})\&quot; and \&quot;list with intersetion relationship(k&#x3D;(v1 v2 v3))\&quot;. The value of range and list can be string(enclosed by \&quot; or &#39;), integer or time(in format \&quot;2020-04-09 02:36:00\&quot;). All of these query patterns should be put in the query string \&quot;q&#x3D;xxx\&quot; and splitted by \&quot;,\&quot;. e.g. q&#x3D;k1&#x3D;v1,k2&#x3D;~v2,k3&#x3D;[min~max] (optional)
     * @param sort Sort the resource list in ascending or descending order. e.g. sort by field1 in ascending orderr and field2 in descending order with \&quot;sort&#x3D;field1,-field2\&quot; (optional)
     * @param page The page number (optional, default to 1)
     * @param pageSize The size of per page (optional, default to 10)
     * @param status The status of webhook job. (optional)
     * @return ApiResponse<kotlin.collections.List<WebhookJob>?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun listWebhookJobsWithHttpInfo(
        projectNameOrId: kotlin.String,
        policyId: kotlin.Long,
        xRequestId: kotlin.String?,
        xIsResourceName: kotlin.Boolean?,
        q: kotlin.String?,
        sort: kotlin.String?,
        page: kotlin.Long?,
        pageSize: kotlin.Long?,
        status: kotlin.collections.List<kotlin.String>?
    ): ApiResponse<kotlin.collections.List<WebhookJob>?> {
        val localVariableConfig = listWebhookJobsRequestConfig(
            projectNameOrId = projectNameOrId,
            policyId = policyId,
            xRequestId = xRequestId,
            xIsResourceName = xIsResourceName,
            q = q,
            sort = sort,
            page = page,
            pageSize = pageSize,
            status = status
        )

        return request<Unit, kotlin.collections.List<WebhookJob>>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation listWebhookJobs
     *
     * @param projectNameOrId The name or id of the project
     * @param policyId The policy ID.
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @param q Query string to query resources. Supported query patterns are \&quot;exact match(k&#x3D;v)\&quot;, \&quot;fuzzy match(k&#x3D;~v)\&quot;, \&quot;range(k&#x3D;[min~max])\&quot;, \&quot;list with union releationship(k&#x3D;{v1 v2 v3})\&quot; and \&quot;list with intersetion relationship(k&#x3D;(v1 v2 v3))\&quot;. The value of range and list can be string(enclosed by \&quot; or &#39;), integer or time(in format \&quot;2020-04-09 02:36:00\&quot;). All of these query patterns should be put in the query string \&quot;q&#x3D;xxx\&quot; and splitted by \&quot;,\&quot;. e.g. q&#x3D;k1&#x3D;v1,k2&#x3D;~v2,k3&#x3D;[min~max] (optional)
     * @param sort Sort the resource list in ascending or descending order. e.g. sort by field1 in ascending orderr and field2 in descending order with \&quot;sort&#x3D;field1,-field2\&quot; (optional)
     * @param page The page number (optional, default to 1)
     * @param pageSize The size of per page (optional, default to 10)
     * @param status The status of webhook job. (optional)
     * @return RequestConfig
     */
    fun listWebhookJobsRequestConfig(
        projectNameOrId: kotlin.String,
        policyId: kotlin.Long,
        xRequestId: kotlin.String?,
        xIsResourceName: kotlin.Boolean?,
        q: kotlin.String?,
        sort: kotlin.String?,
        page: kotlin.Long?,
        pageSize: kotlin.Long?,
        status: kotlin.collections.List<kotlin.String>?
    ): RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf<kotlin.String, kotlin.collections.List<kotlin.String>>()
            .apply {
                if (q != null) {
                    put("q", listOf(q.toString()))
                }
                if (sort != null) {
                    put("sort", listOf(sort.toString()))
                }
                if (page != null) {
                    put("page", listOf(page.toString()))
                }
                if (pageSize != null) {
                    put("page_size", listOf(pageSize.toString()))
                }
                put("policy_id", listOf(policyId.toString()))
                if (status != null) {
                    put("status", toMultiValue(status.toList(), "csv"))
                }
            }
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }
        xIsResourceName?.apply { localVariableHeaders["X-Is-Resource-Name"] = this.toString() }
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/projects/{project_name_or_id}/webhook/jobs".replace(
                "{" + "project_name_or_id" + "}",
                projectNameOrId.toString()
            ),
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

}
