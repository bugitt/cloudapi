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

import cn.edu.buaa.scs.controller.models.ContainerServicePort
import cn.edu.buaa.scs.controller.models.KvPair

/**
 * 
 * @param id 
 * @param name 
 * @param image 
 * @param resourcePoolId 
 * @param resourceUsedRecordId 
 * @param command 
 * @param workingDir 
 * @param envs 
 * @param ports 
 */
data class ContainerResponse(
    val id: kotlin.Long,
    val name: kotlin.String,
    val image: kotlin.String,
    val resourcePoolId: kotlin.String,
    val resourceUsedRecordId: kotlin.String,
    val command: kotlin.String? = null,
    val workingDir: kotlin.String? = null,
    val envs: kotlin.collections.List<KvPair>? = null,
    val ports: kotlin.collections.List<ContainerServicePort>? = null
) 

