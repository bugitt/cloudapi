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
 * @param name The name of this registration
 * @param url A base URL of the scanner adapter.
 * @param auth Specify what authentication approach is adopted for the HTTP communications. Supported types Basic\", \"Bearer\" and api key header \"X-ScannerAdapter-API-Key\" 
 * @param accessCredential An optional value of the HTTP Authorization header sent with each request to the Scanner Adapter API. 
 */

data class ScannerRegistrationSettings (

    /* The name of this registration */
    @field:JsonProperty("name")
    val name: kotlin.String,

    /* A base URL of the scanner adapter. */
    @field:JsonProperty("url")
    val url: java.net.URI,

    /* Specify what authentication approach is adopted for the HTTP communications. Supported types Basic\", \"Bearer\" and api key header \"X-ScannerAdapter-API-Key\"  */
    @field:JsonProperty("auth")
    val auth: kotlin.String? = "",

    /* An optional value of the HTTP Authorization header sent with each request to the Scanner Adapter API.  */
    @field:JsonProperty("access_credential")
    val accessCredential: kotlin.String? = null

)
