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
 * @param categoryName
 * @param segments
 */
data class GetContainerServiceTemplates200ResponseInner(
    val categoryName: kotlin.String,
    val segments: kotlin.collections.List<GetContainerServiceTemplates200ResponseInnerSegmentsInner>
) 

