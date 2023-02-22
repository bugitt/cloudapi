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

import cn.edu.buaa.scs.controller.models.VmNetInfo

/**
 * 
 * @param platform 
 * @param name 
 * @param isTemplate 
 * @param adminId 
 * @param studentId 
 * @param teacherId 
 * @param isExperimental 
 * @param experimentId 
 * @param applyId 
 * @param memory MB
 * @param cpu 
 * @param diskNum 
 * @param diskSize byte
 * @param state creating, booting, running, stopped, shuttingdown,deleting
 * @param netInfos 
 * @param id 
 * @param uuid 
 * @param host 
 * @param osFullName 
 * @param overallStatus gray, green, yellow, red
 */
data class VirtualMachine(
    val platform: kotlin.String,
    val name: kotlin.String,
    val isTemplate: kotlin.Boolean,
    val adminId: kotlin.String,
    val studentId: kotlin.String,
    val teacherId: kotlin.String,
    val isExperimental: kotlin.Boolean,
    val experimentId: kotlin.Int,
    val applyId: kotlin.String,
    /* MB */
    val memory: kotlin.Int,
    val cpu: kotlin.Int,
    val diskNum: kotlin.Int,
    /* byte */
    val diskSize: kotlin.Long,
    /* creating, booting, running, stopped, shuttingdown,deleting */
    val state: kotlin.String,
    val netInfos: kotlin.collections.List<VmNetInfo>,
    val id: kotlin.String,
    val uuid: kotlin.String? = null,
    val host: kotlin.String? = null,
    val osFullName: kotlin.String? = null,
    /* gray, green, yellow, red */
    val overallStatus: kotlin.String? = null
) 

