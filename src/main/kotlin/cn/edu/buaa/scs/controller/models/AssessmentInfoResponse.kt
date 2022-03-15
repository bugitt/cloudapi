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

import cn.edu.buaa.scs.controller.models.SimpleUser

/**
 * 
 * @param assessor 
 * @param assignmentId 
 * @param score 
 * @param assessedTime 
 * @param reason 评分理由，没有理由的也要返回空String
 */
data class AssessmentInfoResponse(
    val assessor: SimpleUser,
    val assignmentId: kotlin.Int,
    val score: kotlin.Double,
    val assessedTime: kotlin.Long,
    /* 评分理由，没有理由的也要返回空String */
    val reason: kotlin.String
) 

