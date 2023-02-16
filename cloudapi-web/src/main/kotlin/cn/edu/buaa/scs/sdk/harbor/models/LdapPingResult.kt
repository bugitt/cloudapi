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
 * The ldap ping result
 *
 * @param success Test success
 * @param message The ping operation output message.
 */

data class LdapPingResult (

    /* Test success */
    @field:JsonProperty("success")
    val success: kotlin.Boolean? = null,

    /* The ping operation output message. */
    @field:JsonProperty("message")
    val message: kotlin.String? = null

)
