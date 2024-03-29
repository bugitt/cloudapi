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
 * @param label 
 * @param name 
 * @param type string, number, boolean
 * @param required 
 * @param options 
 * @param default 
 * @param description 
 */
data class ContainerServiceTemplateConfigItem(
    val label: kotlin.String,
    val name: kotlin.String,
    /* string, number, boolean */
    val type: kotlin.String,
    val required: kotlin.Boolean,
    val options: kotlin.collections.List<kotlin.String>? = null,
    val default: kotlin.String? = null,
    val description: kotlin.String? = null
) 

