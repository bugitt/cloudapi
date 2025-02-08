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
 * @param chatId 
 * @param stream 
 * @param detail 
 * @param messages 
 * @param customUid 
 */
data class ChatCompletionRequest(
    val chatId: kotlin.String,
    val stream: kotlin.Boolean,
    val detail: kotlin.Boolean,
    val messages: kotlin.collections.List<ChatCompletionRequestMessagesInner>,
    val customUid: kotlin.String? = null
) 

