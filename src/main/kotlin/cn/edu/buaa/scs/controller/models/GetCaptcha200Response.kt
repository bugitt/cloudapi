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
 * @param token 
 * @param image base64编码后的png图片
 */
data class GetCaptcha200Response(
    val token: kotlin.String,
    /* base64编码后的png图片 */
    val image: kotlin.String
) 

