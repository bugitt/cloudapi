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

import cn.edu.buaa.scs.controller.models.ImageBuilderMetadata
import cn.edu.buaa.scs.controller.models.ImageBuilderSpec
import cn.edu.buaa.scs.controller.models.ImageBuilderStatus

/**
 * Image builder CRD
 * @param apiVersion APIVersion defines the versioned schema of this representation of an object. Servers should convert recognized schemas to the latest internal value, and may reject unrecognized values. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#resources
 * @param kind Kind is a string value representing the REST resource this object represents. Servers may infer this from the endpoint the client submits requests to. Cannot be updated. In CamelCase. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#types-kinds
 * @param metadata
 * @param spec
 * @param status
 */
data class ImageBuilder(
    /* APIVersion defines the versioned schema of this representation of an object. Servers should convert recognized schemas to the latest internal value, and may reject unrecognized values. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#resources */
    val apiVersion: kotlin.String,
    /* Kind is a string value representing the REST resource this object represents. Servers may infer this from the endpoint the client submits requests to. Cannot be updated. In CamelCase. More info: https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#types-kinds */
    val kind: kotlin.String,
    val metadata: ImageBuilderMetadata,
    val spec: ImageBuilderSpec,
    val status: ImageBuilderStatus? = null
)

