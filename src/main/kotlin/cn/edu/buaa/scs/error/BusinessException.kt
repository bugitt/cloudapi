package cn.edu.buaa.scs.error

open class BusinessException(msg: String, cause: Throwable = Exception()) : Exception(msg, cause)

class RemoteServiceException(val status: Int, msg: String) :
    BusinessException("status:\t$status\nmessage:\t$msg")

class TimeoutException(msg: String) : BusinessException(msg)

class ExecCommandException(msg: String, cause: Throwable) : BusinessException(msg, cause)
