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


/**
 * 
 * @param studentIdList 需要为实验中的学生新增虚拟机时，使用该字段
 */
data class PatchVmApplyVms(
    /* 需要为实验中的学生新增虚拟机时，使用该字段 */
    val studentIdList: kotlin.collections.List<kotlin.String>? = null
) 
