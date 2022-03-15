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

import cn.edu.buaa.scs.controller.models.AssessmentInfoResponse
import cn.edu.buaa.scs.controller.models.FileResponse

/**
 * 
 * @param id AssignmentId
 * @param file 
 * @param peerInfo 
 */
data class StudentPeerTaskResponse(
    /* AssignmentId */
    val id: kotlin.Int,
    val file: FileResponse,
    val peerInfo: AssessmentInfoResponse? = null
) 

