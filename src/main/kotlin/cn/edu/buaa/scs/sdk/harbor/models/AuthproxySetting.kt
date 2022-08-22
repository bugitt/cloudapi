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
 * @param endpoint The fully qualified URI of login endpoint of authproxy, such as 'https://192.168.1.2:8443/login'
 * @param tokenreivewEndpoint The fully qualified URI of token review endpoint of authproxy, such as 'https://192.168.1.2:8443/tokenreview'
 * @param skipSearch The flag to determine whether Harbor can skip search the user/group when adding him as a member.
 * @param verifyCert The flag to determine whether Harbor should verify the certificate when connecting to the auth proxy.
 * @param serverCertificate The certificate to be pinned when connecting auth proxy.
 */

data class AuthproxySetting (

    /* The fully qualified URI of login endpoint of authproxy, such as 'https://192.168.1.2:8443/login' */
    @field:JsonProperty("endpoint")
    val endpoint: kotlin.String? = null,

    /* The fully qualified URI of token review endpoint of authproxy, such as 'https://192.168.1.2:8443/tokenreview' */
    @field:JsonProperty("tokenreivew_endpoint")
    val tokenreivewEndpoint: kotlin.String? = null,

    /* The flag to determine whether Harbor can skip search the user/group when adding him as a member. */
    @field:JsonProperty("skip_search")
    val skipSearch: kotlin.Boolean? = null,

    /* The flag to determine whether Harbor should verify the certificate when connecting to the auth proxy. */
    @field:JsonProperty("verify_cert")
    val verifyCert: kotlin.Boolean? = null,

    /* The certificate to be pinned when connecting auth proxy. */
    @field:JsonProperty("server_certificate")
    val serverCertificate: kotlin.String? = null

)

