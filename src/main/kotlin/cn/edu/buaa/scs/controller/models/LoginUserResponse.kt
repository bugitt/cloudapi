/**
 * cloudapi_v2
 * buaa scs cloud api v2
 *
 * The version of the OpenAPI document: 2.0
 * Contact: loheagn@icloud.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package cn.edu.buaa.scs.controller.models


/**
 *
 * @param userId
 * @param username
 * @param role
 * @param isAssistant
 * @param token
 * @param paasToken
 */
data class LoginUserResponse(
    val userId: kotlin.String,
    val username: kotlin.String,
    val role: kotlin.String,
    val isAssistant: kotlin.Boolean,
    val token: kotlin.String,
    val paasToken: kotlin.String
) 
