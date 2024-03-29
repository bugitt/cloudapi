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

import cn.edu.buaa.scs.controller.models.ContainerResponse

/**
 * 
 * @param id 
 * @param name 
 * @param creator 
 * @param projectId 
 * @param projectName 
 * @param serviceType SERVICE, JOB
 * @param containers 
 * @param createdTime 
 * @param templateId 
 * @param status UNDO, NOT_READY, RUNNING, SUCCESS, FAIL
 */
data class ContainerServiceResponse(
    val id: kotlin.Long,
    val name: kotlin.String,
    val creator: kotlin.String,
    val projectId: kotlin.Long,
    val projectName: kotlin.String,
    /* SERVICE, JOB */
    val serviceType: kotlin.String,
    val containers: kotlin.collections.List<ContainerResponse>,
    val createdTime: kotlin.Long,
    val templateId: kotlin.String? = null,
    /* UNDO, NOT_READY, RUNNING, SUCCESS, FAIL */
    val status: kotlin.String? = null
) 

