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
 * @param email 
 * @param realname 
 * @param comment 
 * @param password 
 * @param username 
 */

data class UserCreationReq (

    @field:JsonProperty("email")
    val email: kotlin.String? = null,

    @field:JsonProperty("realname")
    val realname: kotlin.String? = null,

    @field:JsonProperty("comment")
    val comment: kotlin.String? = null,

    @field:JsonProperty("password")
    val password: kotlin.String? = null,

    @field:JsonProperty("username")
    val username: kotlin.String? = null

)
