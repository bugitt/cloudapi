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
 * The metadata of chart version
 *
 * @param name The name of the chart
 * @param version A SemVer 2 version of chart
 * @param engine The name of template engine
 * @param icon The URL to an icon file
 * @param apiVersion The API version of this chart
 * @param appVersion The version of the application enclosed in the chart
 * @param home The URL to the relevant project page
 * @param sources The URL to the source code of chart
 * @param description A one-sentence description of chart
 * @param keywords A list of string keywords
 * @param deprecated Whether or not this chart is deprecated
 */

data class ChartMetadata (

    /* The name of the chart */
    @field:JsonProperty("name")
    val name: kotlin.String,

    /* A SemVer 2 version of chart */
    @field:JsonProperty("version")
    val version: kotlin.String,

    /* The name of template engine */
    @field:JsonProperty("engine")
    val engine: kotlin.String,

    /* The URL to an icon file */
    @field:JsonProperty("icon")
    val icon: kotlin.String,

    /* The API version of this chart */
    @field:JsonProperty("apiVersion")
    val apiVersion: kotlin.String,

    /* The version of the application enclosed in the chart */
    @field:JsonProperty("appVersion")
    val appVersion: kotlin.String,

    /* The URL to the relevant project page */
    @field:JsonProperty("home")
    val home: kotlin.String? = null,

    /* The URL to the source code of chart */
    @field:JsonProperty("sources")
    val sources: kotlin.collections.List<kotlin.String>? = null,

    /* A one-sentence description of chart */
    @field:JsonProperty("description")
    val description: kotlin.String? = null,

    /* A list of string keywords */
    @field:JsonProperty("keywords")
    val keywords: kotlin.collections.List<kotlin.String>? = null,

    /* Whether or not this chart is deprecated */
    @field:JsonProperty("deprecated")
    val deprecated: kotlin.Boolean? = null

)

