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
 * 当学生读取该Model时，assessor为空，表示学生不允许读取是谁评阅了自己的作业
 * @param assessor 
 * @param assignmentId 
 * @param score 
 * @param adjustedScore 调整后的分数
 * @param assessedTime 
 * @param reason 评分理由，没有理由的也要返回空String
 */
data class AssessmentInfoResponse(
    val assessor: SimpleUser? = null,
    val assignmentId: kotlin.Int? = null,
    val score: kotlin.Double? = null,
    /* 调整后的分数 */
    val adjustedScore: kotlin.Double? = null,
    val assessedTime: kotlin.Long? = null,
    /* 评分理由，没有理由的也要返回空String */
    val reason: kotlin.String? = null
) 
