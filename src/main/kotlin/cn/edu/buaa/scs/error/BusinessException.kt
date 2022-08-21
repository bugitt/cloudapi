package cn.edu.buaa.scs.error

open class BusinessException(msg: String) : Exception(msg)

class RemoteServiceException(val status: Int, msg: String) :
    BusinessException("status:\t$status\nmessage:\t$msg")
