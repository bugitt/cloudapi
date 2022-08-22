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
 * The parameters of the policy, the values are dependent on the type of the policy.
 *
 * @param dailyTime The offset in seconds of UTC 0 o'clock, only valid when the policy type is \"daily\"
 */

data class ConfigurationsResponseScanAllPolicyParameter (

    /* The offset in seconds of UTC 0 o'clock, only valid when the policy type is \"daily\" */
    @field:JsonProperty("daily_time")
    val dailyTime: kotlin.Int? = null

)

