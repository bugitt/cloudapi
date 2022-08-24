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
 * @param uid the uid can't add to system.
 * @param error fail reason.
 */

data class LdapFailedImportUser (

    /* the uid can't add to system. */
    @field:JsonProperty("uid")
    val uid: kotlin.String? = null,

    /* fail reason. */
    @field:JsonProperty("error")
    val error: kotlin.String? = null

)
