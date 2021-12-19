package cn.edu.buaa.scs.utils


fun <T> fail(msg: String): Result<T> =
    Result.failure(BusinessException(msg))

fun <T> success(value: T): Result<T> =
    Result.success(value)