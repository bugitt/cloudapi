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
 * @param type The replication policy filter type.
 * @param `value` The value of replication policy filter.
 * @param decoration matches or excludes the result
 */

data class ReplicationFilter (

    /* The replication policy filter type. */
    @field:JsonProperty("type")
    val type: kotlin.String? = null,

    /* The value of replication policy filter. */
    @field:JsonProperty("value")
    val `value`: kotlin.Any? = null,

    /* matches or excludes the result */
    @field:JsonProperty("decoration")
    val decoration: kotlin.String? = null

)

