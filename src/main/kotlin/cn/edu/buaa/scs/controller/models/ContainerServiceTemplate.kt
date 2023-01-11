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

import cn.edu.buaa.scs.controller.models.ContainerServiceTemplateConfigItem

/**
 * 
 * @param id 
 * @param name 
 * @param category 
 * @param config 
 * @param segment 
 * @param description 
 * @param iconUrl 
 */
data class ContainerServiceTemplate(
    val id: kotlin.String,
    val name: kotlin.String,
    val category: kotlin.String,
    val config: kotlin.collections.List<ContainerServiceTemplateConfigItem>,
    val segment: kotlin.String? = null,
    val description: kotlin.String? = null,
    val iconUrl: kotlin.String? = null
) 

