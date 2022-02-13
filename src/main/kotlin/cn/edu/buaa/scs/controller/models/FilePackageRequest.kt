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
 * @param fileType 
 * @param involvedId 关联的实体ID
 * @param fileIdList 
 */
data class FilePackageRequest(
    val fileType: kotlin.String,
    /* 关联的实体ID */
    val involvedId: kotlin.Int,
    val fileIdList: kotlin.collections.List<kotlin.Int>? = null
) 

