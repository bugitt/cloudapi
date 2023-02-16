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

import cn.edu.buaa.scs.controller.models.Image

/**
 * 
 * @param name 
 * @param artifactsCount 
 * @param downloadCount 
 * @param updateTime 
 * @param images 
 */
data class ImageRepo(
    val name: kotlin.String,
    val artifactsCount: kotlin.Long,
    val downloadCount: kotlin.Long,
    val updateTime: kotlin.Long? = null,
    val images: kotlin.collections.List<Image>? = null
) 
