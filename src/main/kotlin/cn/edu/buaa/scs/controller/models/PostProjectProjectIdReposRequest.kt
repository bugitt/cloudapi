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
 * @param name 
 * @param `private` 
 * @param description 
 * @param gitignores 
 * @param license 
 */
data class PostProjectProjectIdReposRequest(
    val name: kotlin.String,
    val `private`: kotlin.Boolean,
    val description: kotlin.String? = null,
    val gitignores: kotlin.String? = null,
    val license: kotlin.String? = null
) 

