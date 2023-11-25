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

import cn.edu.buaa.scs.controller.models.TermModel

/**
 * 
 * @param id 
 * @param name 
 * @param teacher 任课教师姓名
 * @param term 
 * @param createTime 
 * @param departmentId 
 * @param departmentName 
 * @param studentCnt 本门课的学生人数
 */
data class CourseResponse(
    val id: kotlin.Int,
    val name: kotlin.String,
    /* 任课教师姓名 */
    val teacher: kotlin.String,
    val term: TermModel,
    val createTime: kotlin.String,
    val departmentId: kotlin.String,
    val departmentName: kotlin.String,
    /* 本门课的学生人数 */
    val studentCnt: kotlin.Int? = null
) 

