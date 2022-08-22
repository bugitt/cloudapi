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
 * the tag retention metadata
 *
 * @param templates templates
 * @param scopeSelectors supported scope selectors
 * @param tagSelectors supported tag selectors
 */

data class RetentionMetadata(

    /* templates */
    @field:JsonProperty("templates")
    val templates: kotlin.collections.List<RetentionRuleMetadata>? = null,

    /* supported scope selectors */
    @field:JsonProperty("scope_selectors")
    val scopeSelectors: kotlin.collections.List<RetentionSelectorMetadata>? = null,

    /* supported tag selectors */
    @field:JsonProperty("tag_selectors")
    val tagSelectors: kotlin.collections.List<RetentionSelectorMetadata>? = null

)

