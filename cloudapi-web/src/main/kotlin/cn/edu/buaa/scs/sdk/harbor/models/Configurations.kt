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
 * @param authMode The auth mode of current system, such as \"db_auth\", \"ldap_auth\", \"oidc_auth\"
 * @param emailFrom The sender name for Email notification.
 * @param emailHost The hostname of SMTP server that sends Email notification.
 * @param emailIdentity By default it's empty so the email_username is picked
 * @param emailInsecure Whether or not the certificate will be verified when Harbor tries to access the email server.
 * @param emailPassword Email password
 * @param emailPort The port of SMTP server
 * @param emailSsl When it''s set to true the system will access Email server via TLS by default.  If it''s set to false, it still will handle \"STARTTLS\" from server side.
 * @param emailUsername The username for authenticate against SMTP server
 * @param ldapBaseDn The Base DN for LDAP binding.
 * @param ldapFilter The filter for LDAP search
 * @param ldapGroupBaseDn The base DN to search LDAP group.
 * @param ldapGroupAdminDn Specify the ldap group which have the same privilege with Harbor admin
 * @param ldapGroupAttributeName The attribute which is used as identity of the LDAP group, default is cn.'
 * @param ldapGroupSearchFilter The filter to search the ldap group
 * @param ldapGroupSearchScope The scope to search ldap group. ''0-LDAP_SCOPE_BASE, 1-LDAP_SCOPE_ONELEVEL, 2-LDAP_SCOPE_SUBTREE''
 * @param ldapScope The scope to search ldap users,'0-LDAP_SCOPE_BASE, 1-LDAP_SCOPE_ONELEVEL, 2-LDAP_SCOPE_SUBTREE'
 * @param ldapSearchDn The DN of the user to do the search.
 * @param ldapSearchPassword The password of the ldap search dn
 * @param ldapTimeout Timeout in seconds for connection to LDAP server
 * @param ldapUid The attribute which is used as identity for the LDAP binding, such as \"CN\" or \"SAMAccountname\"
 * @param ldapUrl The URL of LDAP server
 * @param ldapVerifyCert Whether verify your OIDC server certificate, disable it if your OIDC server is hosted via self-hosted certificate.
 * @param ldapGroupMembershipAttribute The user attribute to identify the group membership
 * @param projectCreationRestriction Indicate who can create projects, it could be ''adminonly'' or ''everyone''.
 * @param readOnly The flag to indicate whether Harbor is in readonly mode.
 * @param selfRegistration Whether the Harbor instance supports self-registration.  If it''s set to false, admin need to add user to the instance.
 * @param tokenExpiration The expiration time of the token for internal Registry, in minutes.
 * @param uaaClientId The client id of UAA
 * @param uaaClientSecret The client secret of the UAA
 * @param uaaEndpoint The endpoint of the UAA
 * @param uaaVerifyCert Verify the certificate in UAA server
 * @param httpAuthproxyEndpoint The endpoint of the HTTP auth
 * @param httpAuthproxyTokenreviewEndpoint The token review endpoint
 * @param httpAuthproxyAdminGroups The group which has the harbor admin privileges
 * @param httpAuthproxyAdminUsernames The username which has the harbor admin privileges
 * @param httpAuthproxyVerifyCert Verify the HTTP auth provider's certificate
 * @param httpAuthproxySkipSearch Search user before onboard
 * @param httpAuthproxyServerCertificate The certificate of the HTTP auth provider
 * @param oidcName The OIDC provider name
 * @param oidcEndpoint The endpoint of the OIDC provider
 * @param oidcClientId The client ID of the OIDC provider
 * @param oidcClientSecret The OIDC provider secret
 * @param oidcGroupsClaim The attribute claims the group name
 * @param oidcAdminGroup The OIDC group which has the harbor admin privileges
 * @param oidcScope The scope of the OIDC provider
 * @param oidcUserClaim The attribute claims the username
 * @param oidcVerifyCert Verify the OIDC provider's certificate'
 * @param oidcAutoOnboard Auto onboard the OIDC user
 * @param oidcExtraRedirectParms Extra parameters to add when redirect request to OIDC provider
 * @param robotTokenDuration The robot account token duration in days
 * @param robotNamePrefix The rebot account name prefix
 * @param notificationEnable Enable notification
 * @param quotaPerProjectEnable Enable quota per project
 * @param storagePerProject The storage quota per project
 * @param auditLogForwardEndpoint The audit log forward endpoint
 * @param skipAuditLogDatabase Skip audit log database
 */

data class Configurations (

    /* The auth mode of current system, such as \"db_auth\", \"ldap_auth\", \"oidc_auth\" */
    @field:JsonProperty("auth_mode")
    val authMode: kotlin.String? = null,

    /* The sender name for Email notification. */
    @field:JsonProperty("email_from")
    val emailFrom: kotlin.String? = null,

    /* The hostname of SMTP server that sends Email notification. */
    @field:JsonProperty("email_host")
    val emailHost: kotlin.String? = null,

    /* By default it's empty so the email_username is picked */
    @field:JsonProperty("email_identity")
    val emailIdentity: kotlin.String? = null,

    /* Whether or not the certificate will be verified when Harbor tries to access the email server. */
    @field:JsonProperty("email_insecure")
    val emailInsecure: kotlin.Boolean? = null,

    /* Email password */
    @field:JsonProperty("email_password")
    val emailPassword: kotlin.String? = null,

    /* The port of SMTP server */
    @field:JsonProperty("email_port")
    val emailPort: kotlin.Int? = null,

    /* When it''s set to true the system will access Email server via TLS by default.  If it''s set to false, it still will handle \"STARTTLS\" from server side. */
    @field:JsonProperty("email_ssl")
    val emailSsl: kotlin.Boolean? = null,

    /* The username for authenticate against SMTP server */
    @field:JsonProperty("email_username")
    val emailUsername: kotlin.String? = null,

    /* The Base DN for LDAP binding. */
    @field:JsonProperty("ldap_base_dn")
    val ldapBaseDn: kotlin.String? = null,

    /* The filter for LDAP search */
    @field:JsonProperty("ldap_filter")
    val ldapFilter: kotlin.String? = null,

    /* The base DN to search LDAP group. */
    @field:JsonProperty("ldap_group_base_dn")
    val ldapGroupBaseDn: kotlin.String? = null,

    /* Specify the ldap group which have the same privilege with Harbor admin */
    @field:JsonProperty("ldap_group_admin_dn")
    val ldapGroupAdminDn: kotlin.String? = null,

    /* The attribute which is used as identity of the LDAP group, default is cn.' */
    @field:JsonProperty("ldap_group_attribute_name")
    val ldapGroupAttributeName: kotlin.String? = null,

    /* The filter to search the ldap group */
    @field:JsonProperty("ldap_group_search_filter")
    val ldapGroupSearchFilter: kotlin.String? = null,

    /* The scope to search ldap group. ''0-LDAP_SCOPE_BASE, 1-LDAP_SCOPE_ONELEVEL, 2-LDAP_SCOPE_SUBTREE'' */
    @field:JsonProperty("ldap_group_search_scope")
    val ldapGroupSearchScope: kotlin.Int? = null,

    /* The scope to search ldap users,'0-LDAP_SCOPE_BASE, 1-LDAP_SCOPE_ONELEVEL, 2-LDAP_SCOPE_SUBTREE' */
    @field:JsonProperty("ldap_scope")
    val ldapScope: kotlin.Int? = null,

    /* The DN of the user to do the search. */
    @field:JsonProperty("ldap_search_dn")
    val ldapSearchDn: kotlin.String? = null,

    /* The password of the ldap search dn */
    @field:JsonProperty("ldap_search_password")
    val ldapSearchPassword: kotlin.String? = null,

    /* Timeout in seconds for connection to LDAP server */
    @field:JsonProperty("ldap_timeout")
    val ldapTimeout: kotlin.Int? = null,

    /* The attribute which is used as identity for the LDAP binding, such as \"CN\" or \"SAMAccountname\" */
    @field:JsonProperty("ldap_uid")
    val ldapUid: kotlin.String? = null,

    /* The URL of LDAP server */
    @field:JsonProperty("ldap_url")
    val ldapUrl: kotlin.String? = null,

    /* Whether verify your OIDC server certificate, disable it if your OIDC server is hosted via self-hosted certificate. */
    @field:JsonProperty("ldap_verify_cert")
    val ldapVerifyCert: kotlin.Boolean? = null,

    /* The user attribute to identify the group membership */
    @field:JsonProperty("ldap_group_membership_attribute")
    val ldapGroupMembershipAttribute: kotlin.String? = null,

    /* Indicate who can create projects, it could be ''adminonly'' or ''everyone''. */
    @field:JsonProperty("project_creation_restriction")
    val projectCreationRestriction: kotlin.String? = null,

    /* The flag to indicate whether Harbor is in readonly mode. */
    @field:JsonProperty("read_only")
    val readOnly: kotlin.Boolean? = null,

    /* Whether the Harbor instance supports self-registration.  If it''s set to false, admin need to add user to the instance. */
    @field:JsonProperty("self_registration")
    val selfRegistration: kotlin.Boolean? = null,

    /* The expiration time of the token for internal Registry, in minutes. */
    @field:JsonProperty("token_expiration")
    val tokenExpiration: kotlin.Int? = null,

    /* The client id of UAA */
    @field:JsonProperty("uaa_client_id")
    val uaaClientId: kotlin.String? = null,

    /* The client secret of the UAA */
    @field:JsonProperty("uaa_client_secret")
    val uaaClientSecret: kotlin.String? = null,

    /* The endpoint of the UAA */
    @field:JsonProperty("uaa_endpoint")
    val uaaEndpoint: kotlin.String? = null,

    /* Verify the certificate in UAA server */
    @field:JsonProperty("uaa_verify_cert")
    val uaaVerifyCert: kotlin.Boolean? = null,

    /* The endpoint of the HTTP auth */
    @field:JsonProperty("http_authproxy_endpoint")
    val httpAuthproxyEndpoint: kotlin.String? = null,

    /* The token review endpoint */
    @field:JsonProperty("http_authproxy_tokenreview_endpoint")
    val httpAuthproxyTokenreviewEndpoint: kotlin.String? = null,

    /* The group which has the harbor admin privileges */
    @field:JsonProperty("http_authproxy_admin_groups")
    val httpAuthproxyAdminGroups: kotlin.String? = null,

    /* The username which has the harbor admin privileges */
    @field:JsonProperty("http_authproxy_admin_usernames")
    val httpAuthproxyAdminUsernames: kotlin.String? = null,

    /* Verify the HTTP auth provider's certificate */
    @field:JsonProperty("http_authproxy_verify_cert")
    val httpAuthproxyVerifyCert: kotlin.Boolean? = null,

    /* Search user before onboard */
    @field:JsonProperty("http_authproxy_skip_search")
    val httpAuthproxySkipSearch: kotlin.Boolean? = null,

    /* The certificate of the HTTP auth provider */
    @field:JsonProperty("http_authproxy_server_certificate")
    val httpAuthproxyServerCertificate: kotlin.String? = null,

    /* The OIDC provider name */
    @field:JsonProperty("oidc_name")
    val oidcName: kotlin.String? = null,

    /* The endpoint of the OIDC provider */
    @field:JsonProperty("oidc_endpoint")
    val oidcEndpoint: kotlin.String? = null,

    /* The client ID of the OIDC provider */
    @field:JsonProperty("oidc_client_id")
    val oidcClientId: kotlin.String? = null,

    /* The OIDC provider secret */
    @field:JsonProperty("oidc_client_secret")
    val oidcClientSecret: kotlin.String? = null,

    /* The attribute claims the group name */
    @field:JsonProperty("oidc_groups_claim")
    val oidcGroupsClaim: kotlin.String? = null,

    /* The OIDC group which has the harbor admin privileges */
    @field:JsonProperty("oidc_admin_group")
    val oidcAdminGroup: kotlin.String? = null,

    /* The scope of the OIDC provider */
    @field:JsonProperty("oidc_scope")
    val oidcScope: kotlin.String? = null,

    /* The attribute claims the username */
    @field:JsonProperty("oidc_user_claim")
    val oidcUserClaim: kotlin.String? = null,

    /* Verify the OIDC provider's certificate' */
    @field:JsonProperty("oidc_verify_cert")
    val oidcVerifyCert: kotlin.Boolean? = null,

    /* Auto onboard the OIDC user */
    @field:JsonProperty("oidc_auto_onboard")
    val oidcAutoOnboard: kotlin.Boolean? = null,

    /* Extra parameters to add when redirect request to OIDC provider */
    @field:JsonProperty("oidc_extra_redirect_parms")
    val oidcExtraRedirectParms: kotlin.String? = null,

    /* The robot account token duration in days */
    @field:JsonProperty("robot_token_duration")
    val robotTokenDuration: kotlin.Int? = null,

    /* The rebot account name prefix */
    @field:JsonProperty("robot_name_prefix")
    val robotNamePrefix: kotlin.String? = null,

    /* Enable notification */
    @field:JsonProperty("notification_enable")
    val notificationEnable: kotlin.Boolean? = null,

    /* Enable quota per project */
    @field:JsonProperty("quota_per_project_enable")
    val quotaPerProjectEnable: kotlin.Boolean? = null,

    /* The storage quota per project */
    @field:JsonProperty("storage_per_project")
    val storagePerProject: kotlin.Int? = null,

    /* The audit log forward endpoint */
    @field:JsonProperty("audit_log_forward_endpoint")
    val auditLogForwardEndpoint: kotlin.String? = null,

    /* Skip audit log database */
    @field:JsonProperty("skip_audit_log_database")
    val skipAuditLogDatabase: kotlin.Boolean? = null

)

