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
 * @param cpu 
 * @param memory MB
 * @param diskSize bytes
 * @param templateUuid 创建虚拟机所使用的模板的UUID
 * @param description 申请理由
 * @param namePrefix 生成的虚拟机的名称的前缀
 * @param dueTime 使用截止时间
 * @param studentId 当学生为自己申请一个不与实验相关的虚拟机时，需设置此字段
 * @param teacherId 当教师为自己申请一个不与实验相关的虚拟机时，需设置此字段
 * @param experimentId 当教师或助教为实验申请虚拟机时，需要使用此字段，并且，如果设置了此字段，同样要设置 studentIdList 字段，表示具体为哪些学生分配虚拟机
 * @param studentIdList 表示具体为哪些学生分配虚拟机。需要与experimentId字段结合使用
 */
data class CreateVmApplyRequest(
    val cpu: kotlin.Int,
    /* MB */
    val memory: kotlin.Int,
    /* bytes */
    val diskSize: kotlin.Long,
    /* 创建虚拟机所使用的模板的UUID */
    val templateUuid: kotlin.String,
    /* 申请理由 */
    val description: kotlin.String,
    /* 生成的虚拟机的名称的前缀 */
    val namePrefix: kotlin.String,
    /* 使用截止时间 */
    val dueTime: kotlin.Long,
    /* 当学生为自己申请一个不与实验相关的虚拟机时，需设置此字段 */
    val studentId: kotlin.String? = null,
    /* 当教师为自己申请一个不与实验相关的虚拟机时，需设置此字段 */
    val teacherId: kotlin.String? = null,
    /* 当教师或助教为实验申请虚拟机时，需要使用此字段，并且，如果设置了此字段，同样要设置 studentIdList 字段，表示具体为哪些学生分配虚拟机 */
    val experimentId: kotlin.Int? = null,
    /* 表示具体为哪些学生分配虚拟机。需要与experimentId字段结合使用 */
    val studentIdList: kotlin.collections.List<kotlin.String>? = null
) 

