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
 * @param created The created time of the chart entry
 * @param removed A flag to indicate if the chart entry is removed
 * @param digest The digest value of the chart entry
 * @param urls The urls of the chart entry
 */

data class ChartVersionAllOf (

    /* The created time of the chart entry */
    @field:JsonProperty("created")
    val created: kotlin.String? = null,

    /* A flag to indicate if the chart entry is removed */
    @field:JsonProperty("removed")
    val removed: kotlin.Boolean? = null,

    /* The digest value of the chart entry */
    @field:JsonProperty("digest")
    val digest: kotlin.String? = null,

    /* The urls of the chart entry */
    @field:JsonProperty("urls")
    val urls: kotlin.collections.List<kotlin.String>? = null

)

