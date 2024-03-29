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
 * @param authMode
 * @param emailFrom
 * @param emailHost
 * @param emailIdentity
 * @param emailInsecure
 * @param emailPort
 * @param emailSsl
 * @param emailUsername
 * @param ldapBaseDn
 * @param ldapFilter
 * @param ldapGroupBaseDn
 * @param ldapGroupAdminDn
 * @param ldapGroupAttributeName
 * @param ldapGroupSearchFilter
 * @param ldapGroupSearchScope
 * @param ldapScope
 * @param ldapSearchDn
 * @param ldapTimeout
 * @param ldapUid
 * @param ldapUrl
 * @param ldapVerifyCert
 * @param ldapGroupMembershipAttribute
 * @param projectCreationRestriction
 * @param readOnly
 * @param selfRegistration
 * @param tokenExpiration
 * @param uaaClientId
 * @param uaaClientSecret
 * @param uaaEndpoint
 * @param uaaVerifyCert
 * @param httpAuthproxyEndpoint
 * @param httpAuthproxyTokenreviewEndpoint
 * @param httpAuthproxyAdminGroups
 * @param httpAuthproxyAdminUsernames
 * @param httpAuthproxyVerifyCert
 * @param httpAuthproxySkipSearch
 * @param httpAuthproxyServerCertificate
 * @param oidcName
 * @param oidcEndpoint
 * @param oidcClientId
 * @param oidcGroupsClaim
 * @param oidcAdminGroup
 * @param oidcScope
 * @param oidcUserClaim
 * @param oidcVerifyCert
 * @param oidcAutoOnboard
 * @param oidcExtraRedirectParms
 * @param robotTokenDuration
 * @param robotNamePrefix
 * @param notificationEnable
 * @param quotaPerProjectEnable
 * @param storagePerProject
 * @param auditLogForwardEndpoint
 * @param skipAuditLogDatabase
 * @param scanAllPolicy
 */

data class ConfigurationsResponse(

    @field:JsonProperty("auth_mode")
    val authMode: StringConfigItem? = null,

    @field:JsonProperty("email_from")
    val emailFrom: StringConfigItem? = null,

    @field:JsonProperty("email_host")
    val emailHost: StringConfigItem? = null,

    @field:JsonProperty("email_identity")
    val emailIdentity: StringConfigItem? = null,

    @field:JsonProperty("email_insecure")
    val emailInsecure: BoolConfigItem? = null,

    @field:JsonProperty("email_port")
    val emailPort: IntegerConfigItem? = null,

    @field:JsonProperty("email_ssl")
    val emailSsl: BoolConfigItem? = null,

    @field:JsonProperty("email_username")
    val emailUsername: StringConfigItem? = null,

    @field:JsonProperty("ldap_base_dn")
    val ldapBaseDn: StringConfigItem? = null,

    @field:JsonProperty("ldap_filter")
    val ldapFilter: StringConfigItem? = null,

    @field:JsonProperty("ldap_group_base_dn")
    val ldapGroupBaseDn: StringConfigItem? = null,

    @field:JsonProperty("ldap_group_admin_dn")
    val ldapGroupAdminDn: StringConfigItem? = null,

    @field:JsonProperty("ldap_group_attribute_name")
    val ldapGroupAttributeName: StringConfigItem? = null,

    @field:JsonProperty("ldap_group_search_filter")
    val ldapGroupSearchFilter: StringConfigItem? = null,

    @field:JsonProperty("ldap_group_search_scope")
    val ldapGroupSearchScope: IntegerConfigItem? = null,

    @field:JsonProperty("ldap_scope")
    val ldapScope: IntegerConfigItem? = null,

    @field:JsonProperty("ldap_search_dn")
    val ldapSearchDn: StringConfigItem? = null,

    @field:JsonProperty("ldap_timeout")
    val ldapTimeout: IntegerConfigItem? = null,

    @field:JsonProperty("ldap_uid")
    val ldapUid: StringConfigItem? = null,

    @field:JsonProperty("ldap_url")
    val ldapUrl: StringConfigItem? = null,

    @field:JsonProperty("ldap_verify_cert")
    val ldapVerifyCert: BoolConfigItem? = null,

    @field:JsonProperty("ldap_group_membership_attribute")
    val ldapGroupMembershipAttribute: StringConfigItem? = null,

    @field:JsonProperty("project_creation_restriction")
    val projectCreationRestriction: StringConfigItem? = null,

    @field:JsonProperty("read_only")
    val readOnly: BoolConfigItem? = null,

    @field:JsonProperty("self_registration")
    val selfRegistration: BoolConfigItem? = null,

    @field:JsonProperty("token_expiration")
    val tokenExpiration: IntegerConfigItem? = null,

    @field:JsonProperty("uaa_client_id")
    val uaaClientId: StringConfigItem? = null,

    @field:JsonProperty("uaa_client_secret")
    val uaaClientSecret: StringConfigItem? = null,

    @field:JsonProperty("uaa_endpoint")
    val uaaEndpoint: StringConfigItem? = null,

    @field:JsonProperty("uaa_verify_cert")
    val uaaVerifyCert: BoolConfigItem? = null,

    @field:JsonProperty("http_authproxy_endpoint")
    val httpAuthproxyEndpoint: StringConfigItem? = null,

    @field:JsonProperty("http_authproxy_tokenreview_endpoint")
    val httpAuthproxyTokenreviewEndpoint: StringConfigItem? = null,

    @field:JsonProperty("http_authproxy_admin_groups")
    val httpAuthproxyAdminGroups: StringConfigItem? = null,

    @field:JsonProperty("http_authproxy_admin_usernames")
    val httpAuthproxyAdminUsernames: StringConfigItem? = null,

    @field:JsonProperty("http_authproxy_verify_cert")
    val httpAuthproxyVerifyCert: BoolConfigItem? = null,

    @field:JsonProperty("http_authproxy_skip_search")
    val httpAuthproxySkipSearch: BoolConfigItem? = null,

    @field:JsonProperty("http_authproxy_server_certificate")
    val httpAuthproxyServerCertificate: StringConfigItem? = null,

    @field:JsonProperty("oidc_name")
    val oidcName: StringConfigItem? = null,

    @field:JsonProperty("oidc_endpoint")
    val oidcEndpoint: StringConfigItem? = null,

    @field:JsonProperty("oidc_client_id")
    val oidcClientId: StringConfigItem? = null,

    @field:JsonProperty("oidc_groups_claim")
    val oidcGroupsClaim: StringConfigItem? = null,

    @field:JsonProperty("oidc_admin_group")
    val oidcAdminGroup: StringConfigItem? = null,

    @field:JsonProperty("oidc_scope")
    val oidcScope: StringConfigItem? = null,

    @field:JsonProperty("oidc_user_claim")
    val oidcUserClaim: StringConfigItem? = null,

    @field:JsonProperty("oidc_verify_cert")
    val oidcVerifyCert: BoolConfigItem? = null,

    @field:JsonProperty("oidc_auto_onboard")
    val oidcAutoOnboard: BoolConfigItem? = null,

    @field:JsonProperty("oidc_extra_redirect_parms")
    val oidcExtraRedirectParms: StringConfigItem? = null,

    @field:JsonProperty("robot_token_duration")
    val robotTokenDuration: IntegerConfigItem? = null,

    @field:JsonProperty("robot_name_prefix")
    val robotNamePrefix: StringConfigItem? = null,

    @field:JsonProperty("notification_enable")
    val notificationEnable: BoolConfigItem? = null,

    @field:JsonProperty("quota_per_project_enable")
    val quotaPerProjectEnable: BoolConfigItem? = null,

    @field:JsonProperty("storage_per_project")
    val storagePerProject: IntegerConfigItem? = null,

    @field:JsonProperty("audit_log_forward_endpoint")
    val auditLogForwardEndpoint: StringConfigItem? = null,

    @field:JsonProperty("skip_audit_log_database")
    val skipAuditLogDatabase: BoolConfigItem? = null,

    @field:JsonProperty("scan_all_policy")
    val scanAllPolicy: ConfigurationsResponseScanAllPolicy? = null

)

