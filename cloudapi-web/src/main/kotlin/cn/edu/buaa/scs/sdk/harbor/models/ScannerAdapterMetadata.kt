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
 * The metadata info of the scanner adapter
 *
 * @param scanner
 * @param capabilities
 * @param properties
 */

data class ScannerAdapterMetadata(

    @field:JsonProperty("scanner")
    val scanner: Scanner? = null,

    @field:JsonProperty("capabilities")
    val capabilities: kotlin.collections.List<ScannerCapability>? = null,

    @field:JsonProperty("properties")
    val properties: kotlin.collections.Map<kotlin.String, kotlin.String>? = null

)
