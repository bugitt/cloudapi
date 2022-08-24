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
import cn.edu.buaa.scs.sdk.harbor.models.Icon
import okhttp3.OkHttpClient
import java.io.IOException

class IconApi(basePath: kotlin.String = defaultBasePath, client: OkHttpClient = ApiClient.defaultClient) :
    ApiClient(basePath, client) {
    companion object {
        @JvmStatic
        val defaultBasePath: String by lazy {
            System.getProperties().getProperty(ApiClient.baseUrlKey, "https://localhost/api/v2.0")
        }
    }

    /**
     * Get artifact icon
     * Get the artifact icon with the specified digest. As the original icon image is resized and encoded before returning, the parameter \&quot;digest\&quot; in the path doesn&#39;t match the hash of the returned content
     * @param digest The digest of the resource
     * @param xRequestId An unique ID for the request (optional)
     * @return Icon
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
    fun getIcon(digest: kotlin.String, xRequestId: kotlin.String? = null): Icon {
        val localVarResponse = getIconWithHttpInfo(digest = digest, xRequestId = xRequestId)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as Icon
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
     * Get artifact icon
     * Get the artifact icon with the specified digest. As the original icon image is resized and encoded before returning, the parameter \&quot;digest\&quot; in the path doesn&#39;t match the hash of the returned content
     * @param digest The digest of the resource
     * @param xRequestId An unique ID for the request (optional)
     * @return ApiResponse<Icon?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun getIconWithHttpInfo(digest: kotlin.String, xRequestId: kotlin.String?): ApiResponse<Icon?> {
        val localVariableConfig = getIconRequestConfig(digest = digest, xRequestId = xRequestId)

        return request<Unit, Icon>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getIcon
     *
     * @param digest The digest of the resource
     * @param xRequestId An unique ID for the request (optional)
     * @return RequestConfig
     */
    fun getIconRequestConfig(digest: kotlin.String, xRequestId: kotlin.String?): RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/icons/{digest}".replace("{" + "digest" + "}", digest.toString()),
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

}