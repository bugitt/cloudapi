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
 * @param id 
 * @param policyId 
 * @param startTime 
 * @param endTime 
 * @param status 
 * @param trigger 
 * @param dryRun 
 */

data class RetentionExecution (

    @field:JsonProperty("id")
    val id: kotlin.Long? = null,

    @field:JsonProperty("policy_id")
    val policyId: kotlin.Long? = null,

    @field:JsonProperty("start_time")
    val startTime: kotlin.String? = null,

    @field:JsonProperty("end_time")
    val endTime: kotlin.String? = null,

    @field:JsonProperty("status")
    val status: kotlin.String? = null,

    @field:JsonProperty("trigger")
    val trigger: kotlin.String? = null,

    @field:JsonProperty("dry_run")
    val dryRun: kotlin.Boolean? = null

)
