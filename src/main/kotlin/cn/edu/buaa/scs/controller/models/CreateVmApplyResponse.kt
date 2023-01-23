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
 * @param id 
 * @param studentId 
 * @param teacherId 
 * @param experimentId 
 * @param studentIdList 
 * @param cpu 
 * @param memory MB
 * @param diskSize bytes
 * @param templateUuid 创建虚拟机所使用的模板的UUID
 * @param description 申请理由
 * @param applyTime 发起申请时的时间戳
 * @param status 0，表示还没有被处理 1，表示允许同意申请 2，表示拒绝申请
 * @param handleTime 管理员处理该申请的时间，时间戳
 * @param expectedNum 预期希望得到的虚拟机个数
 * @param actualNum 当前实际的虚拟机个数
 * @param namePrefix 最终生成的虚拟机的名称的前缀
 * @param dueTime 虚拟机使用的结束时间
 * @param replyMsg 管理员审批该申请的回复信息
 */
data class CreateVmApplyResponse(
    val id: kotlin.String,
    val studentId: kotlin.String,
    val teacherId: kotlin.String,
    val experimentId: kotlin.Int,
    val studentIdList: kotlin.collections.List<kotlin.String>,
    val cpu: kotlin.Int,
    /* MB */
    val memory: kotlin.Int,
    /* bytes */
    val diskSize: kotlin.Long,
    /* 创建虚拟机所使用的模板的UUID */
    val templateUuid: kotlin.String,
    /* 申请理由 */
    val description: kotlin.String,
    /* 发起申请时的时间戳 */
    val applyTime: kotlin.Long,
    /* 0，表示还没有被处理 1，表示允许同意申请 2，表示拒绝申请 */
    val status: kotlin.Int,
    /* 管理员处理该申请的时间，时间戳 */
    val handleTime: kotlin.Long,
    /* 预期希望得到的虚拟机个数 */
    val expectedNum: kotlin.Int,
    /* 当前实际的虚拟机个数 */
    val actualNum: kotlin.Int,
    /* 最终生成的虚拟机的名称的前缀 */
    val namePrefix: kotlin.String,
    /* 虚拟机使用的结束时间 */
    val dueTime: kotlin.Long? = null,
    /* 管理员审批该申请的回复信息 */
    val replyMsg: kotlin.String? = null
) 

