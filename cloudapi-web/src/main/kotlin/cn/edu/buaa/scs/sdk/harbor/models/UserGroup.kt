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
 * @param id The ID of the user group
 * @param groupName The name of the user group
 * @param groupType The group type, 1 for LDAP group, 2 for HTTP group, 3 for OIDC group.
 * @param ldapGroupDn The DN of the LDAP group if group type is 1 (LDAP group).
 */

data class UserGroup (

    /* The ID of the user group */
    @field:JsonProperty("id")
    val id: kotlin.Int? = null,

    /* The name of the user group */
    @field:JsonProperty("group_name")
    val groupName: kotlin.String? = null,

    /* The group type, 1 for LDAP group, 2 for HTTP group, 3 for OIDC group. */
    @field:JsonProperty("group_type")
    val groupType: kotlin.Int? = null,

    /* The DN of the LDAP group if group type is 1 (LDAP group). */
    @field:JsonProperty("ldap_group_dn")
    val ldapGroupDn: kotlin.String? = null

)

