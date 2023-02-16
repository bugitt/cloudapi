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
 * The item in CVE allowlist
 *
 * @param cveId The ID of the CVE, such as \"CVE-2019-10164\"
 */

data class CVEAllowlistItem (

    /* The ID of the CVE, such as \"CVE-2019-10164\" */
    @field:JsonProperty("cve_id")
    val cveId: kotlin.String? = null

)
