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
 * The registry provider info contains the base info and capability declarations of the registry provider
 *
 * @param endpointPattern
 * @param credentialPattern
 */

data class RegistryProviderInfo(

    @field:JsonProperty("endpoint_pattern")
    val endpointPattern: RegistryProviderEndpointPattern? = null,

    @field:JsonProperty("credential_pattern")
    val credentialPattern: RegistryProviderCredentialPattern? = null

)

