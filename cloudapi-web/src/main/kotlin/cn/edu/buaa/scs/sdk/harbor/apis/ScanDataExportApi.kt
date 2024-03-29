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
import cn.edu.buaa.scs.sdk.harbor.models.ScanDataExportExecution
import cn.edu.buaa.scs.sdk.harbor.models.ScanDataExportExecutionList
import cn.edu.buaa.scs.sdk.harbor.models.ScanDataExportJob
import cn.edu.buaa.scs.sdk.harbor.models.ScanDataExportRequest
import okhttp3.OkHttpClient
import java.io.IOException

class ScanDataExportApi(basePath: kotlin.String = defaultBasePath, client: OkHttpClient = ApiClient.defaultClient) :
    ApiClient(basePath, client) {
    companion object {
        @JvmStatic
        val defaultBasePath: String by lazy {
            System.getProperties().getProperty(ApiClient.baseUrlKey, "https://localhost/api/v2.0")
        }
    }

    /**
     * Download the scan data export file
     * Download the scan data report. Default format is CSV
     * @param executionId Execution ID
     * @param xRequestId An unique ID for the request (optional)
     * @param format The format of the data to be exported. e.g. CSV or PDF (optional)
     * @return java.io.File
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
    fun downloadScanData(
        executionId: kotlin.Int,
        xRequestId: kotlin.String? = null,
        format: kotlin.String? = null
    ): java.io.File {
        val localVarResponse =
            downloadScanDataWithHttpInfo(executionId = executionId, xRequestId = xRequestId, format = format)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as java.io.File
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
     * Download the scan data export file
     * Download the scan data report. Default format is CSV
     * @param executionId Execution ID
     * @param xRequestId An unique ID for the request (optional)
     * @param format The format of the data to be exported. e.g. CSV or PDF (optional)
     * @return ApiResponse<java.io.File?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun downloadScanDataWithHttpInfo(
        executionId: kotlin.Int,
        xRequestId: kotlin.String?,
        format: kotlin.String?
    ): ApiResponse<java.io.File?> {
        val localVariableConfig =
            downloadScanDataRequestConfig(executionId = executionId, xRequestId = xRequestId, format = format)

        return request<Unit, java.io.File>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation downloadScanData
     *
     * @param executionId Execution ID
     * @param xRequestId An unique ID for the request (optional)
     * @param format The format of the data to be exported. e.g. CSV or PDF (optional)
     * @return RequestConfig
     */
    fun downloadScanDataRequestConfig(
        executionId: kotlin.Int,
        xRequestId: kotlin.String?,
        format: kotlin.String?
    ): RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf<kotlin.String, kotlin.collections.List<kotlin.String>>()
            .apply {
                if (format != null) {
                    put("format", listOf(format.toString()))
                }
            }
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/export/cve/download/{execution_id}".replace("{" + "execution_id" + "}", executionId.toString()),
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

    /**
     * Export scan data for selected projects
     * Export scan data for selected projects
     * @param xScanDataType The type of scan data to export
     * @param criteria The criteria for the export
     * @param xRequestId An unique ID for the request (optional)
     * @return ScanDataExportJob
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
    fun exportScanData(
        xScanDataType: kotlin.String,
        criteria: ScanDataExportRequest,
        xRequestId: kotlin.String? = null
    ): ScanDataExportJob {
        val localVarResponse =
            exportScanDataWithHttpInfo(xScanDataType = xScanDataType, criteria = criteria, xRequestId = xRequestId)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as ScanDataExportJob
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
     * Export scan data for selected projects
     * Export scan data for selected projects
     * @param xScanDataType The type of scan data to export
     * @param criteria The criteria for the export
     * @param xRequestId An unique ID for the request (optional)
     * @return ApiResponse<ScanDataExportJob?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun exportScanDataWithHttpInfo(
        xScanDataType: kotlin.String,
        criteria: ScanDataExportRequest,
        xRequestId: kotlin.String?
    ): ApiResponse<ScanDataExportJob?> {
        val localVariableConfig =
            exportScanDataRequestConfig(xScanDataType = xScanDataType, criteria = criteria, xRequestId = xRequestId)

        return request<ScanDataExportRequest, ScanDataExportJob>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation exportScanData
     *
     * @param xScanDataType The type of scan data to export
     * @param criteria The criteria for the export
     * @param xRequestId An unique ID for the request (optional)
     * @return RequestConfig
     */
    fun exportScanDataRequestConfig(
        xScanDataType: kotlin.String,
        criteria: ScanDataExportRequest,
        xRequestId: kotlin.String?
    ): RequestConfig<ScanDataExportRequest> {
        val localVariableBody = criteria
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }
        xScanDataType.apply { localVariableHeaders["X-Scan-Data-Type"] = this.toString() }
        localVariableHeaders["Content-Type"] = "application/json"
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.POST,
            path = "/export/cve",
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

    /**
     * Get the specific scan data export execution
     * Get the scan data export execution specified by ID
     * @param executionId Execution ID
     * @param xRequestId An unique ID for the request (optional)
     * @return ScanDataExportExecution
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
    fun getScanDataExportExecution(
        executionId: kotlin.Int,
        xRequestId: kotlin.String? = null
    ): ScanDataExportExecution {
        val localVarResponse =
            getScanDataExportExecutionWithHttpInfo(executionId = executionId, xRequestId = xRequestId)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as ScanDataExportExecution
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
     * Get the specific scan data export execution
     * Get the scan data export execution specified by ID
     * @param executionId Execution ID
     * @param xRequestId An unique ID for the request (optional)
     * @return ApiResponse<ScanDataExportExecution?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun getScanDataExportExecutionWithHttpInfo(
        executionId: kotlin.Int,
        xRequestId: kotlin.String?
    ): ApiResponse<ScanDataExportExecution?> {
        val localVariableConfig =
            getScanDataExportExecutionRequestConfig(executionId = executionId, xRequestId = xRequestId)

        return request<Unit, ScanDataExportExecution>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getScanDataExportExecution
     *
     * @param executionId Execution ID
     * @param xRequestId An unique ID for the request (optional)
     * @return RequestConfig
     */
    fun getScanDataExportExecutionRequestConfig(
        executionId: kotlin.Int,
        xRequestId: kotlin.String?
    ): RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/export/cve/execution/{execution_id}".replace("{" + "execution_id" + "}", executionId.toString()),
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

    /**
     * Get a list of specific scan data export execution jobs for a specified user
     * Get a list of specific scan data export execution jobs for a specified user
     * @param xRequestId An unique ID for the request (optional)
     * @return ScanDataExportExecutionList
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
    fun getScanDataExportExecutionList(xRequestId: kotlin.String? = null): ScanDataExportExecutionList {
        val localVarResponse = getScanDataExportExecutionListWithHttpInfo(xRequestId = xRequestId)

        return when (localVarResponse.responseType) {
            ResponseType.Success -> (localVarResponse as Success<*>).data as ScanDataExportExecutionList
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
     * Get a list of specific scan data export execution jobs for a specified user
     * Get a list of specific scan data export execution jobs for a specified user
     * @param xRequestId An unique ID for the request (optional)
     * @return ApiResponse<ScanDataExportExecutionList?>
     * @throws IllegalStateException If the request is not correctly configured
     * @throws IOException Rethrows the OkHttp execute method exception
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalStateException::class, IOException::class)
    fun getScanDataExportExecutionListWithHttpInfo(xRequestId: kotlin.String?): ApiResponse<ScanDataExportExecutionList?> {
        val localVariableConfig = getScanDataExportExecutionListRequestConfig(xRequestId = xRequestId)

        return request<Unit, ScanDataExportExecutionList>(
            localVariableConfig
        )
    }

    /**
     * To obtain the request config of the operation getScanDataExportExecutionList
     *
     * @param xRequestId An unique ID for the request (optional)
     * @return RequestConfig
     */
    fun getScanDataExportExecutionListRequestConfig(xRequestId: kotlin.String?): RequestConfig<Unit> {
        val localVariableBody = null
        val localVariableQuery: MultiValueMap = mutableMapOf()
        val localVariableHeaders: MutableMap<String, String> = mutableMapOf()
        xRequestId?.apply { localVariableHeaders["X-Request-Id"] = this.toString() }
        localVariableHeaders["Accept"] = "application/json"

        return RequestConfig(
            method = RequestMethod.GET,
            path = "/export/cve/executions",
            query = localVariableQuery,
            headers = localVariableHeaders,
            body = localVariableBody
        )
    }

}
