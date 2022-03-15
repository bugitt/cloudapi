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
 * @param studentId 
 * @param expId 
 * @param courseId 
 * @param createdAt 长整型时间戳
 * @param updatedAt 长整型时间戳
 * @param file 
 * @param peerScore 
 * @param finalScore 
 */
data class AssignmentResponse(
    val id: kotlin.Int,
    val studentId: kotlin.String,
    val expId: kotlin.Int,
    val courseId: kotlin.Int,
    /* 长整型时间戳 */
    val createdAt: kotlin.Long,
    /* 长整型时间戳 */
    val updatedAt: kotlin.Long,
    val file: FileResponse? = null,
    val peerScore: kotlin.Double? = null,
    val finalScore: kotlin.Double? = null
) 

