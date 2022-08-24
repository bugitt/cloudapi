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
import okhttp3.OkHttpClient
import java.io.IOException

class ProjectMetadataApi(basePath: kotlin.String = defaultBasePath, client: OkHttpClient = ApiClient.defaultClient) :
    ApiClient(basePath, client) {
    companion object {
        @JvmStatic
        val defaultBasePath: String by lazy {
            System.getProperties().getProperty(ApiClient.baseUrlKey, "https://localhost/api/v2.0")
        }
    }

    /**
     * Add metadata for the specific project
     * Add metadata for the specific project
     * @param projectNameOrId The name or id of the project
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @param metadata  (optional)
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
    fun addProjectMetadatas(
        projectNameOrId: kotlin.String,
        xRequestId: kotlin.String? = null,
        xIsResourceName: kotlin.Boolean? = false,
        metadata: kotlin.collections.Map<kotlin.String, kotlin.String>? = null
    ): Unit {
        val localVarResponse = addProjectMetadatasWithHttpInfo(
            projectNameOrId = projectNameOrId,
            xRequestId = xRequestId,
            xIsResourceName = xIsResourceName,
            metadata = metadata
        )

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
     * Add metadata for the specific project
     * Add metadata for the specific project
     * @param projectNameOrId The name or id of the project
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @param metadata  (optional)
     * @return ApiResponse<Unit?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Throws(IllegalStateException::class, IOException::class)
    fun addProjectMetadatasWithHttpInfo(
        projectNameOrId: kotlin.String,
        xRequestId: kotlin.String?,
        xIsResourceName: kotlin.Boolean?,
        metadata: kotlin.collections.Map<kotlin.String, kotlin.String>?
    ): ApiResponse<Unit?> {
        val localVariableConfig = addProjectMetadatasRequestConfig(
            projectNameOrId = projectNameOrId,
            xRequestId = xRequestId,
            xIsResourceName = xIsResourceName,
            metadata = metadata
        )

        return request<kotlin.collections.Map<kotlin.String, kotlin.String>, Unit>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation addProjectMetadatas
     *
     * @param projectNameOrId The name or id of the project
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @param metadata  (optional)
     * @return RequestConfig
     */
    fun addProjectMetadatasRequestConfig(
        projectNameOrId: kotlin.String,
        xRequestId: kotlin.String?,
        xIsResourceName: kotlin.Boolean?,
        metadata: kotlin.collections.Map<kotlin.String, kotlin.String>?
    ): RequestConfig<kotlin.collections.Map<kotlin.String, kotlin.String>> {
        val localVariableBody = metadata
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }
        xIsResourceName?.apply { localVariableHeaders["X-Is-Resource-Name"] = this.toString() }
        localVariableHeaders["Content-Type"] = "application/json"
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.POST,
            path = "/projects/{project_name_or_id}/metadatas/".replace(
                "{" + "project_name_or_id" + "}",
                projectNameOrId.toString()
            ),
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

    /**
     * Delete the specific metadata for the specific project
     * Delete the specific metadata for the specific project
     * @param projectNameOrId The name or id of the project
     * @param metaName The name of metadata.
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
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
    fun deleteProjectMetadata(
        projectNameOrId: kotlin.String,
        metaName: kotlin.String,
        xRequestId: kotlin.String? = null,
        xIsResourceName: kotlin.Boolean? = false
    ): Unit {
        val localVarResponse = deleteProjectMetadataWithHttpInfo(
            projectNameOrId = projectNameOrId,
            metaName = metaName,
            xRequestId = xRequestId,
            xIsResourceName = xIsResourceName
        )

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
     * Delete the specific metadata for the specific project
     * Delete the specific metadata for the specific project
     * @param projectNameOrId The name or id of the project
     * @param metaName The name of metadata.
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @return ApiResponse<Unit?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Throws(IllegalStateException::class, IOException::class)
    fun deleteProjectMetadataWithHttpInfo(
        projectNameOrId: kotlin.String,
        metaName: kotlin.String,
        xRequestId: kotlin.String?,
        xIsResourceName: kotlin.Boolean?
    ): ApiResponse<Unit?> {
        val localVariableConfig = deleteProjectMetadataRequestConfig(
            projectNameOrId = projectNameOrId,
            metaName = metaName,
            xRequestId = xRequestId,
            xIsResourceName = xIsResourceName
        )

        return request<Unit, Unit>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation deleteProjectMetadata
     *
     * @param projectNameOrId The name or id of the project
     * @param metaName The name of metadata.
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @return RequestConfig
     */
    fun deleteProjectMetadataRequestConfig(
        projectNameOrId: kotlin.String,
        metaName: kotlin.String,
        xRequestId: kotlin.String?,
        xIsResourceName: kotlin.Boolean?
    ): RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }
        xIsResourceName?.apply { localVariableHeaders["X-Is-Resource-Name"] = this.toString() }
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.DELETE,
            path = "/projects/{project_name_or_id}/metadatas/{meta_name}".replace(
                "{" + "project_name_or_id" + "}",
                projectNameOrId.toString()
            ).replace("{" + "meta_name" + "}", metaName.toString()),
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

    /**
     * Get the specific metadata of the specific project
     * Get the specific metadata of the specific project
     * @param projectNameOrId The name or id of the project
     * @param metaName The name of metadata.
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @return kotlin.collections.Map<kotlin.String, kotlin.String>
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
    fun getProjectMetadata(
        projectNameOrId: kotlin.String,
        metaName: kotlin.String,
        xRequestId: kotlin.String? = null,
        xIsResourceName: kotlin.Boolean? = false
    ): kotlin.collections.Map<kotlin.String, kotlin.String> {
        val localVarResponse = getProjectMetadataWithHttpInfo(
            projectNameOrId = projectNameOrId,
            metaName = metaName,
            xRequestId = xRequestId,
            xIsResourceName = xIsResourceName
        )

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as kotlin.collections.Map<kotlin.String, kotlin.String>
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
     * Get the specific metadata of the specific project
     * Get the specific metadata of the specific project
     * @param projectNameOrId The name or id of the project
     * @param metaName The name of metadata.
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @return ApiResponse<kotlin.collections.Map<kotlin.String, kotlin.String>?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun getProjectMetadataWithHttpInfo(
        projectNameOrId: kotlin.String,
        metaName: kotlin.String,
        xRequestId: kotlin.String?,
        xIsResourceName: kotlin.Boolean?
    ): ApiResponse<kotlin.collections.Map<kotlin.String, kotlin.String>?> {
        val localVariableConfig = getProjectMetadataRequestConfig(
            projectNameOrId = projectNameOrId,
            metaName = metaName,
            xRequestId = xRequestId,
            xIsResourceName = xIsResourceName
        )

        return request<Unit, kotlin.collections.Map<kotlin.String, kotlin.String>>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getProjectMetadata
     *
     * @param projectNameOrId The name or id of the project
     * @param metaName The name of metadata.
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @return RequestConfig
     */
    fun getProjectMetadataRequestConfig(
        projectNameOrId: kotlin.String,
        metaName: kotlin.String,
        xRequestId: kotlin.String?,
        xIsResourceName: kotlin.Boolean?
    ): RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }
        xIsResourceName?.apply { localVariableHeaders["X-Is-Resource-Name"] = this.toString() }
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/projects/{project_name_or_id}/metadatas/{meta_name}".replace(
                "{" + "project_name_or_id" + "}",
                projectNameOrId.toString()
            ).replace("{" + "meta_name" + "}", metaName.toString()),
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

    /**
     * Get the metadata of the specific project
     * Get the metadata of the specific project
     * @param projectNameOrId The name or id of the project
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @return kotlin.collections.Map<kotlin.String, kotlin.String>
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
    fun listProjectMetadatas(
        projectNameOrId: kotlin.String,
        xRequestId: kotlin.String? = null,
        xIsResourceName: kotlin.Boolean? = false
    ): kotlin.collections.Map<kotlin.String, kotlin.String> {
        val localVarResponse = listProjectMetadatasWithHttpInfo(
            projectNameOrId = projectNameOrId,
            xRequestId = xRequestId,
            xIsResourceName = xIsResourceName
        )

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as kotlin.collections.Map<kotlin.String, kotlin.String>
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
     * Get the metadata of the specific project
     * Get the metadata of the specific project
     * @param projectNameOrId The name or id of the project
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @return ApiResponse<kotlin.collections.Map<kotlin.String, kotlin.String>?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun listProjectMetadatasWithHttpInfo(
        projectNameOrId: kotlin.String,
        xRequestId: kotlin.String?,
        xIsResourceName: kotlin.Boolean?
    ): ApiResponse<kotlin.collections.Map<kotlin.String, kotlin.String>?> {
        val localVariableConfig = listProjectMetadatasRequestConfig(
            projectNameOrId = projectNameOrId,
            xRequestId = xRequestId,
            xIsResourceName = xIsResourceName
        )

        return request<Unit, kotlin.collections.Map<kotlin.String, kotlin.String>>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation listProjectMetadatas
     *
     * @param projectNameOrId The name or id of the project
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @return RequestConfig
     */
    fun listProjectMetadatasRequestConfig(
        projectNameOrId: kotlin.String,
        xRequestId: kotlin.String?,
        xIsResourceName: kotlin.Boolean?
    ): RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }
        xIsResourceName?.apply { localVariableHeaders["X-Is-Resource-Name"] = this.toString() }
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/projects/{project_name_or_id}/metadatas/".replace(
                "{" + "project_name_or_id" + "}",
                projectNameOrId.toString()
            ),
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

    /**
     * Update the specific metadata for the specific project
     * Update the specific metadata for the specific project
     * @param projectNameOrId The name or id of the project
     * @param metaName The name of metadata.
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @param metadata  (optional)
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
    fun updateProjectMetadata(
        projectNameOrId: kotlin.String,
        metaName: kotlin.String,
        xRequestId: kotlin.String? = null,
        xIsResourceName: kotlin.Boolean? = false,
        metadata: kotlin.collections.Map<kotlin.String, kotlin.String>? = null
    ): Unit {
        val localVarResponse = updateProjectMetadataWithHttpInfo(
            projectNameOrId = projectNameOrId,
            metaName = metaName,
            xRequestId = xRequestId,
            xIsResourceName = xIsResourceName,
            metadata = metadata
        )

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
     * Update the specific metadata for the specific project
     * Update the specific metadata for the specific project
     * @param projectNameOrId The name or id of the project
     * @param metaName The name of metadata.
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @param metadata  (optional)
     * @return ApiResponse<Unit?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Throws(IllegalStateException::class, IOException::class)
    fun updateProjectMetadataWithHttpInfo(
        projectNameOrId: kotlin.String,
        metaName: kotlin.String,
        xRequestId: kotlin.String?,
        xIsResourceName: kotlin.Boolean?,
        metadata: kotlin.collections.Map<kotlin.String, kotlin.String>?
    ): ApiResponse<Unit?> {
        val localVariableConfig = updateProjectMetadataRequestConfig(
            projectNameOrId = projectNameOrId,
            metaName = metaName,
            xRequestId = xRequestId,
            xIsResourceName = xIsResourceName,
            metadata = metadata
        )

        return request<kotlin.collections.Map<kotlin.String, kotlin.String>, Unit>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation updateProjectMetadata
     *
     * @param projectNameOrId The name or id of the project
     * @param metaName The name of metadata.
     * @param xRequestId An unique ID for the request (optional)
     * @param xIsResourceName The flag to indicate whether the parameter which supports both name and id in the path is the name of the resource. When the X-Is-Resource-Name is false and the parameter can be converted to an integer, the parameter will be as an id, otherwise, it will be as a name. (optional, default to false)
     * @param metadata  (optional)
     * @return RequestConfig
     */
    fun updateProjectMetadataRequestConfig(
        projectNameOrId: kotlin.String,
        metaName: kotlin.String,
        xRequestId: kotlin.String?,
        xIsResourceName: kotlin.Boolean?,
        metadata: kotlin.collections.Map<kotlin.String, kotlin.String>?
    ): RequestConfig<kotlin.collections.Map<kotlin.String, kotlin.String>> {
        val localVariableBody = metadata
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }
        xIsResourceName?.apply { localVariableHeaders["X-Is-Resource-Name"] = this.toString() }
        localVariableHeaders["Content-Type"] = "application/json"
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.PUT,
            path = "/projects/{project_name_or_id}/metadatas/{meta_name}".replace(
                "{" + "project_name_or_id" + "}",
                projectNameOrId.toString()
            ).replace("{" + "meta_name" + "}", metaName.toString()),
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

}