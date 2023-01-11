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
 * @param accessKeyID 
 * @param accessSecretKey 
 * @param bucket 
 * @param objectKey 
 * @param region 
 * @param endpoint 
 * @param fileType 
 * @param scheme 
 */
data class ImageBuilderSpecContextS3(
    val accessKeyID: kotlin.String,
    val accessSecretKey: kotlin.String,
    val bucket: kotlin.String,
    val objectKey: kotlin.String,
    val region: kotlin.String,
    val endpoint: kotlin.String? = "s3.amazonaws.com",
    val fileType: ImageBuilderSpecContextS3.FileType? = null,
    val scheme: ImageBuilderSpecContextS3.Scheme? = null
) 
{
    /**
    * 
    * Values: tar,tarPeriodGz,zip,rar,dir
    */
    enum class FileType(val value: kotlin.String){
        tar("tar"),
        tarPeriodGz("tar.gz"),
        zip("zip"),
        rar("rar"),
        dir("dir");
    }
    /**
    * 
    * Values: http,https
    */
    enum class Scheme(val value: kotlin.String){
        http("http"),
        https("https");
    }
}

