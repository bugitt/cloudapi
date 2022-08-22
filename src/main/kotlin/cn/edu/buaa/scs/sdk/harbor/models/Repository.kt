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
 * @param id The ID of the repository
 * @param projectId The ID of the project that the repository belongs to
 * @param name The name of the repository
 * @param description The description of the repository
 * @param artifactCount The count of the artifacts inside the repository
 * @param pullCount The count that the artifact inside the repository pulled
 * @param creationTime The creation time of the repository
 * @param updateTime The update time of the repository
 */

data class Repository (

    /* The ID of the repository */
    @field:JsonProperty("id")
    val id: kotlin.Long? = null,

    /* The ID of the project that the repository belongs to */
    @field:JsonProperty("project_id")
    val projectId: kotlin.Long? = null,

    /* The name of the repository */
    @field:JsonProperty("name")
    val name: kotlin.String? = null,

    /* The description of the repository */
    @field:JsonProperty("description")
    val description: kotlin.String? = null,

    /* The count of the artifacts inside the repository */
    @field:JsonProperty("artifact_count")
    val artifactCount: kotlin.Long? = null,

    /* The count that the artifact inside the repository pulled */
    @field:JsonProperty("pull_count")
    val pullCount: kotlin.Long? = null,

    /* The creation time of the repository */
    @field:JsonProperty("creation_time")
    val creationTime: java.time.OffsetDateTime? = null,

    /* The update time of the repository */
    @field:JsonProperty("update_time")
    val updateTime: java.time.OffsetDateTime? = null

)

