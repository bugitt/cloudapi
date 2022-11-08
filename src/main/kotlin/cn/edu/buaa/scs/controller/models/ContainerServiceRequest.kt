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
 * @param name
 * @param serviceType SERVICE, JOB
 * @param containers
 */
data class ContainerServiceRequest(
    val name: kotlin.String,
    /* SERVICE, JOB */
    val serviceType: kotlin.String,
    val containers: kotlin.collections.List<ContainerRequest>
) 
