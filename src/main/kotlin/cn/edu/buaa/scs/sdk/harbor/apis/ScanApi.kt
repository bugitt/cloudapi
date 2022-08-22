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

class ScanApi(basePath: kotlin.String = defaultBasePath, client: OkHttpClient = ApiClient.defaultClient) :
    ApiClient(basePath, client) {
    companion object {
        @JvmStatic
        val defaultBasePath: String by lazy {
            System.getProperties().getProperty(ApiClient.baseUrlKey, "https://localhost/api/v2.0")
        }
    }

    /**
     * Get the log of the scan report
     * Get the log of the scan report
     * @param projectName The name of the project
     * @param repositoryName The name of the repository. If it contains slash, encode it with URL encoding. e.g. a/b -&gt; a%252Fb
     * @param reference The reference of the artifact, can be digest or tag
     * @param reportId The report id to get the log
     * @param xRequestId An unique ID for the request (optional)
     * @return kotlin.String
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
    fun getReportLog(
        projectName: kotlin.String,
        repositoryName: kotlin.String,
        reference: kotlin.String,
        reportId: kotlin.String,
        xRequestId: kotlin.String? = null
    ): kotlin.String {
        val localVarResponse = getReportLogWithHttpInfo(
            projectName = projectName,
            repositoryName = repositoryName,
            reference = reference,
            reportId = reportId,
            xRequestId = xRequestId
        )

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as kotlin.String
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
     * Get the log of the scan report
     * Get the log of the scan report
     * @param projectName The name of the project
     * @param repositoryName The name of the repository. If it contains slash, encode it with URL encoding. e.g. a/b -&gt; a%252Fb
     * @param reference The reference of the artifact, can be digest or tag
     * @param reportId The report id to get the log
     * @param xRequestId An unique ID for the request (optional)
     * @return ApiResponse<kotlin.String?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun getReportLogWithHttpInfo(
        projectName: kotlin.String,
        repositoryName: kotlin.String,
        reference: kotlin.String,
        reportId: kotlin.String,
        xRequestId: kotlin.String?
    ): ApiResponse<kotlin.String?> {
        val localVariableConfig = getReportLogRequestConfig(
            projectName = projectName,
            repositoryName = repositoryName,
            reference = reference,
            reportId = reportId,
            xRequestId = xRequestId
        )

        return request<Unit, kotlin.String>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getReportLog
     *
     * @param projectName The name of the project
     * @param repositoryName The name of the repository. If it contains slash, encode it with URL encoding. e.g. a/b -&gt; a%252Fb
     * @param reference The reference of the artifact, can be digest or tag
     * @param reportId The report id to get the log
     * @param xRequestId An unique ID for the request (optional)
     * @return RequestConfig
     */
    fun getReportLogRequestConfig(
        projectName: kotlin.String,
        repositoryName: kotlin.String,
        reference: kotlin.String,
        reportId: kotlin.String,
        xRequestId: kotlin.String?
    ): RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/projects/{project_name}/repositories/{repository_name}/artifacts/{reference}/scan/{report_id}/log".replace(
                "{" + "project_name" + "}",
                projectName.toString()
            ).replace("{" + "repository_name" + "}", repositoryName.toString())
                .replace("{" + "reference" + "}", reference.toString())
                .replace("{" + "report_id" + "}", reportId.toString()),
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

    /**
     * Scan the artifact
     * Scan the specified artifact
     * @param projectName The name of the project
     * @param repositoryName The name of the repository. If it contains slash, encode it with URL encoding. e.g. a/b -&gt; a%252Fb
     * @param reference The reference of the artifact, can be digest or tag
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
    fun scanArtifact(
        projectName: kotlin.String,
        repositoryName: kotlin.String,
        reference: kotlin.String,
        xRequestId: kotlin.String? = null
    ): Unit {
        val localVarResponse = scanArtifactWithHttpInfo(
            projectName = projectName,
            repositoryName = repositoryName,
            reference = reference,
            xRequestId = xRequestId
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
     * Scan the artifact
     * Scan the specified artifact
     * @param projectName The name of the project
     * @param repositoryName The name of the repository. If it contains slash, encode it with URL encoding. e.g. a/b -&gt; a%252Fb
     * @param reference The reference of the artifact, can be digest or tag
     * @param xRequestId An unique ID for the request (optional)
     * @return ApiResponse<Unit?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Throws(IllegalStateException::class, IOException::class)
    fun scanArtifactWithHttpInfo(
        projectName: kotlin.String,
        repositoryName: kotlin.String,
        reference: kotlin.String,
        xRequestId: kotlin.String?
    ): ApiResponse<Unit?> {
        val localVariableConfig = scanArtifactRequestConfig(
            projectName = projectName,
            repositoryName = repositoryName,
            reference = reference,
            xRequestId = xRequestId
        )

        return request<Unit, Unit>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation scanArtifact
     *
     * @param projectName The name of the project
     * @param repositoryName The name of the repository. If it contains slash, encode it with URL encoding. e.g. a/b -&gt; a%252Fb
     * @param reference The reference of the artifact, can be digest or tag
     * @param xRequestId An unique ID for the request (optional)
     * @return RequestConfig
     */
    fun scanArtifactRequestConfig(
        projectName: kotlin.String,
        repositoryName: kotlin.String,
        reference: kotlin.String,
        xRequestId: kotlin.String?
    ): RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.POST,
            path = "/projects/{project_name}/repositories/{repository_name}/artifacts/{reference}/scan".replace(
                "{" + "project_name" + "}",
                projectName.toString()
            ).replace("{" + "repository_name" + "}", repositoryName.toString())
                .replace("{" + "reference" + "}", reference.toString()),
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

    /**
     * Cancelling a scan job for a particular artifact
     * Cancelling a scan job for a particular artifact
     * @param projectName The name of the project
     * @param repositoryName The name of the repository. If it contains slash, encode it with URL encoding. e.g. a/b -&gt; a%252Fb
     * @param reference The reference of the artifact, can be digest or tag
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
    fun stopScanArtifact(
        projectName: kotlin.String,
        repositoryName: kotlin.String,
        reference: kotlin.String,
        xRequestId: kotlin.String? = null
    ): Unit {
        val localVarResponse = stopScanArtifactWithHttpInfo(
            projectName = projectName,
            repositoryName = repositoryName,
            reference = reference,
            xRequestId = xRequestId
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
     * Cancelling a scan job for a particular artifact
     * Cancelling a scan job for a particular artifact
     * @param projectName The name of the project
     * @param repositoryName The name of the repository. If it contains slash, encode it with URL encoding. e.g. a/b -&gt; a%252Fb
     * @param reference The reference of the artifact, can be digest or tag
     * @param xRequestId An unique ID for the request (optional)
     * @return ApiResponse<Unit?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Throws(IllegalStateException::class, IOException::class)
    fun stopScanArtifactWithHttpInfo(
        projectName: kotlin.String,
        repositoryName: kotlin.String,
        reference: kotlin.String,
        xRequestId: kotlin.String?
    ): ApiResponse<Unit?> {
        val localVariableConfig = stopScanArtifactRequestConfig(
            projectName = projectName,
            repositoryName = repositoryName,
            reference = reference,
            xRequestId = xRequestId
        )

        return request<Unit, Unit>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation stopScanArtifact
     *
     * @param projectName The name of the project
     * @param repositoryName The name of the repository. If it contains slash, encode it with URL encoding. e.g. a/b -&gt; a%252Fb
     * @param reference The reference of the artifact, can be digest or tag
     * @param xRequestId An unique ID for the request (optional)
     * @return RequestConfig
     */
    fun stopScanArtifactRequestConfig(
        projectName: kotlin.String,
        repositoryName: kotlin.String,
        reference: kotlin.String,
        xRequestId: kotlin.String?
    ): RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.POST,
            path = "/projects/{project_name}/repositories/{repository_name}/artifacts/{reference}/scan/stop".replace(
                "{" + "project_name" + "}",
                projectName.toString()
            ).replace("{" + "repository_name" + "}", repositoryName.toString())
                .replace("{" + "reference" + "}", reference.toString()),
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

}
