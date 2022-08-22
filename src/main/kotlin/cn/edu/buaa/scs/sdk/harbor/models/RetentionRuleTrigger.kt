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
 * @param kind 
 * @param settings 
 * @param references 
 */

data class RetentionRuleTrigger (

    @field:JsonProperty("kind")
    val kind: kotlin.String? = null,

    @field:JsonProperty("settings")
    val settings: kotlin.Any? = null,

    @field:JsonProperty("references")
    val references: kotlin.Any? = null

)

