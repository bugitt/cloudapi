package cn.edu.buaa.scs.cloudapi.error

sealed class CloudApiException(message: String): RuntimeException(message)

class PropertyNotFoundException(name: String): CloudApiException("property $name not found")