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
 * @param name 
 * @param uploadTime 
 * @param fileType 文件类型，枚举值，可选：Assignment
 * @param fileSize 文件大小，长整型
 * @param uploader 
 * @param owner 
 * @param downloadLink 
 * @param createdAt 
 * @param updatedAt 
 * @param contentType mimeType
 * @param involveId 
 */
data class FileResponse(
    val id: kotlin.Int,
    val name: kotlin.String,
    val uploadTime: kotlin.Long,
    /* 文件类型，枚举值，可选：Assignment */
    val fileType: kotlin.String,
    /* 文件大小，长整型 */
    val fileSize: kotlin.Long,
    val uploader: kotlin.String,
    val owner: kotlin.String,
    val downloadLink: java.net.URI,
    val createdAt: kotlin.Long,
    val updatedAt: kotlin.Long,
    /* mimeType */
    val contentType: kotlin.String,
    val involveId: kotlin.Int
) 

