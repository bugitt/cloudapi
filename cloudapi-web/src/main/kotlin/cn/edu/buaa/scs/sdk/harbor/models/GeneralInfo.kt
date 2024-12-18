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
 * @param currentTime The current time of the server.
 * @param withNotary If the Harbor instance is deployed with nested notary.
 * @param withChartmuseum If the Harbor instance is deployed with nested chartmuseum.
 * @param registryUrl The url of registry against which the docker command should be issued.
 * @param externalUrl The external URL of Harbor, with protocol.
 * @param authMode The auth mode of current Harbor instance.
 * @param projectCreationRestriction Indicate who can create projects, it could be 'adminonly' or 'everyone'.
 * @param selfRegistration Indicate whether the Harbor instance enable user to register himself.
 * @param hasCaRoot Indicate whether there is a ca root cert file ready for download in the file system.
 * @param harborVersion The build version of Harbor.
 * @param registryStorageProviderName The storage provider's name of Harbor registry
 * @param readOnly The flag to indicate whether Harbor is in readonly mode.
 * @param notificationEnable The flag to indicate whether notification mechanism is enabled on Harbor instance.
 * @param authproxySettings
 */

data class GeneralInfo(

    /* The current time of the server. */
    @field:JsonProperty("current_time")
    val currentTime: java.time.OffsetDateTime? = null,

    /* If the Harbor instance is deployed with nested notary. */
    @field:JsonProperty("with_notary")
    val withNotary: kotlin.Boolean? = null,

    /* If the Harbor instance is deployed with nested chartmuseum. */
    @field:JsonProperty("with_chartmuseum")
    val withChartmuseum: kotlin.Boolean? = null,

    /* The url of registry against which the docker command should be issued. */
    @field:JsonProperty("registry_url")
    val registryUrl: kotlin.String? = null,

    /* The external URL of Harbor, with protocol. */
    @field:JsonProperty("external_url")
    val externalUrl: kotlin.String? = null,

    /* The auth mode of current Harbor instance. */
    @field:JsonProperty("auth_mode")
    val authMode: kotlin.String? = null,

    /* Indicate who can create projects, it could be 'adminonly' or 'everyone'. */
    @field:JsonProperty("project_creation_restriction")
    val projectCreationRestriction: kotlin.String? = null,

    /* Indicate whether the Harbor instance enable user to register himself. */
    @field:JsonProperty("self_registration")
    val selfRegistration: kotlin.Boolean? = null,

    /* Indicate whether there is a ca root cert file ready for download in the file system. */
    @field:JsonProperty("has_ca_root")
    val hasCaRoot: kotlin.Boolean? = null,

    /* The build version of Harbor. */
    @field:JsonProperty("harbor_version")
    val harborVersion: kotlin.String? = null,

    /* The storage provider's name of Harbor registry */
    @field:JsonProperty("registry_storage_provider_name")
    val registryStorageProviderName: kotlin.String? = null,

    /* The flag to indicate whether Harbor is in readonly mode. */
    @field:JsonProperty("read_only")
    val readOnly: kotlin.Boolean? = null,

    /* The flag to indicate whether notification mechanism is enabled on Harbor instance. */
    @field:JsonProperty("notification_enable")
    val notificationEnable: kotlin.Boolean? = null,

    @field:JsonProperty("authproxy_settings")
    val authproxySettings: AuthproxySetting? = null

)

