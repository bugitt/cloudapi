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
 * @param taskCount The count of task
 * @param successTaskCount The count of success task
 * @param errorTaskCount The count of error task
 * @param pendingTaskCount The count of pending task
 * @param runningTaskCount The count of running task
 * @param scheduledTaskCount The count of scheduled task
 * @param stoppedTaskCount The count of stopped task
 */

data class Metrics (

    /* The count of task */
    @field:JsonProperty("task_count")
    val taskCount: kotlin.Int? = null,

    /* The count of success task */
    @field:JsonProperty("success_task_count")
    val successTaskCount: kotlin.Int? = null,

    /* The count of error task */
    @field:JsonProperty("error_task_count")
    val errorTaskCount: kotlin.Int? = null,

    /* The count of pending task */
    @field:JsonProperty("pending_task_count")
    val pendingTaskCount: kotlin.Int? = null,

    /* The count of running task */
    @field:JsonProperty("running_task_count")
    val runningTaskCount: kotlin.Int? = null,

    /* The count of scheduled task */
    @field:JsonProperty("scheduled_task_count")
    val scheduledTaskCount: kotlin.Int? = null,

    /* The count of stopped task */
    @field:JsonProperty("stopped_task_count")
    val stoppedTaskCount: kotlin.Int? = null

)

