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

import cn.edu.buaa.scs.controller.models.FileResponse

/**
 * 
 * @param id 
 * @param courseId 
 * @param file 
 */
data class CourseResourceResponse(
    val id: kotlin.Int,
    val courseId: kotlin.Int,
    val file: FileResponse
) 

