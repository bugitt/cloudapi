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
 * @param oldPassword The user's existing password.
 * @param newPassword New password for marking as to be updated.
 */

data class PasswordReq (

    /* The user's existing password. */
    @field:JsonProperty("old_password")
    val oldPassword: kotlin.String? = null,

    /* New password for marking as to be updated. */
    @field:JsonProperty("new_password")
    val newPassword: kotlin.String? = null

)

