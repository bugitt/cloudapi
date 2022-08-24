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
 * The CVE Allowlist for system or project
 *
 * @param id ID of the allowlist
 * @param projectId ID of the project which the allowlist belongs to.  For system level allowlist this attribute is zero.
 * @param expiresAt the time for expiration of the allowlist, in the form of seconds since epoch.  This is an optional attribute, if it's not set the CVE allowlist does not expire.
 * @param items
 * @param creationTime The creation time of the allowlist.
 * @param updateTime The update time of the allowlist.
 */

data class CVEAllowlist(

    /* ID of the allowlist */
    @field:JsonProperty("id")
    val id: kotlin.Int? = null,

    /* ID of the project which the allowlist belongs to.  For system level allowlist this attribute is zero. */
    @field:JsonProperty("project_id")
    val projectId: kotlin.Int? = null,

    /* the time for expiration of the allowlist, in the form of seconds since epoch.  This is an optional attribute, if it's not set the CVE allowlist does not expire. */
    @field:JsonProperty("expires_at")
    val expiresAt: kotlin.Int? = null,

    @field:JsonProperty("items")
    val items: kotlin.collections.List<CVEAllowlistItem>? = null,

    /* The creation time of the allowlist. */
    @field:JsonProperty("creation_time")
    val creationTime: java.time.OffsetDateTime? = null,

    /* The update time of the allowlist. */
    @field:JsonProperty("update_time")
    val updateTime: java.time.OffsetDateTime? = null

)
