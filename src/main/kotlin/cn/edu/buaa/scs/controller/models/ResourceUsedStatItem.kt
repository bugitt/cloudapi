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
 * @param name 未使用的份额的name值是 \"空闲\" 否则，其格式应该为 \"${project_name} / ${container_service_name}\"
 * @param `value`
 */
data class ResourceUsedStatItem(
    /* 未使用的份额的name值是 \"空闲\" 否则，其格式应该为 \"${project_name} / ${container_service_name}\" */
    val name: kotlin.String,
    val `value`: kotlin.Int
)

