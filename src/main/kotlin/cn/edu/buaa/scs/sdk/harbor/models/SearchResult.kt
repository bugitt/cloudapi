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
 * The chart search result item
 *
 * @param name The chart name with repo name
 * @param score The matched level
 * @param chart
 */

data class SearchResult(

    /* The chart name with repo name */
    @field:JsonProperty("Name")
    val name: kotlin.String? = null,

    /* The matched level */
    @field:JsonProperty("Score")
    val score: kotlin.Int? = null,

    @field:JsonProperty("Chart")
    val chart: ChartVersion? = null

)

