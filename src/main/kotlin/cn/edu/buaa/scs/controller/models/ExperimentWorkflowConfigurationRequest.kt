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

import cn.edu.buaa.scs.controller.models.ResourceModel

/**
 * 
 * @param id 
 * @param expId 
 * @param resource 
 * @param configuration json string
 */
data class ExperimentWorkflowConfigurationRequest(
    val id: kotlin.Long,
    val expId: kotlin.Int,
    val resource: ResourceModel,
    /* json string */
    val configuration: kotlin.String
) 

