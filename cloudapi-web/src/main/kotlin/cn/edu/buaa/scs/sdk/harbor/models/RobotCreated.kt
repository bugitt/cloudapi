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
 * The response for robot account creation.
 *
 * @param id The ID of the robot
 * @param name The name of the tag
 * @param secret The secret of the robot
 * @param creationTime The creation time of the robot.
 * @param expiresAt The expiration data of the robot
 */

data class RobotCreated (

    /* The ID of the robot */
    @field:JsonProperty("id")
    val id: kotlin.Long? = null,

    /* The name of the tag */
    @field:JsonProperty("name")
    val name: kotlin.String? = null,

    /* The secret of the robot */
    @field:JsonProperty("secret")
    val secret: kotlin.String? = null,

    /* The creation time of the robot. */
    @field:JsonProperty("creation_time")
    val creationTime: java.time.OffsetDateTime? = null,

    /* The expiration data of the robot */
    @field:JsonProperty("expires_at")
    val expiresAt: kotlin.Long? = null

)

