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
 * @param teacherId 需要删除教师申请的单个虚拟机时设置此字段
 * @param studentId 需要删除学生申请的单个虚拟机时设置此字段
 * @param studentIdList 需要删除批量申请的多个虚拟机时设置此字段
 */
data class DeleteVmApplyVms(
    /* 需要删除教师申请的单个虚拟机时设置此字段 */
    val teacherId: kotlin.String? = null,
    /* 需要删除学生申请的单个虚拟机时设置此字段 */
    val studentId: kotlin.String? = null,
    /* 需要删除批量申请的多个虚拟机时设置此字段 */
    val studentIdList: kotlin.collections.List<kotlin.String>? = null
) 

