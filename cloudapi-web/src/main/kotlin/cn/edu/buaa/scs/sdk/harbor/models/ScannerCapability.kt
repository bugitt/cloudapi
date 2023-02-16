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

package cn.edu.buaa.scs.sdk.harbor.models


import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 
 *
 * @param consumesMimeTypes 
 * @param producesMimeTypes 
 */

data class ScannerCapability (

    @field:JsonProperty("consumes_mime_types")
    val consumesMimeTypes: kotlin.collections.List<kotlin.String>? = null,

    @field:JsonProperty("produces_mime_types")
    val producesMimeTypes: kotlin.collections.List<kotlin.String>? = null

)
