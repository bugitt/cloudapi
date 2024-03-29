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
 * @param id The ID of the label
 * @param name The name the label
 * @param description The description the label
 * @param color The color the label
 * @param scope The scope the label
 * @param projectId The ID of project that the label belongs to
 * @param creationTime The creation time the label
 * @param updateTime The update time of the label
 */

data class Label (

    /* The ID of the label */
    @field:JsonProperty("id")
    val id: kotlin.Long? = null,

    /* The name the label */
    @field:JsonProperty("name")
    val name: kotlin.String? = null,

    /* The description the label */
    @field:JsonProperty("description")
    val description: kotlin.String? = null,

    /* The color the label */
    @field:JsonProperty("color")
    val color: kotlin.String? = null,

    /* The scope the label */
    @field:JsonProperty("scope")
    val scope: kotlin.String? = null,

    /* The ID of project that the label belongs to */
    @field:JsonProperty("project_id")
    val projectId: kotlin.Long? = null,

    /* The creation time the label */
    @field:JsonProperty("creation_time")
    val creationTime: java.time.OffsetDateTime? = null,

    /* The update time of the label */
    @field:JsonProperty("update_time")
    val updateTime: java.time.OffsetDateTime? = null

)

