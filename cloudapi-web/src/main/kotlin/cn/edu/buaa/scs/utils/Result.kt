package cn.edu.buaa.scs.utils

import cn.edu.buaa.scs.error.BusinessException

@Suppress("unused")
fun <T> fail(msg: String): Result<T> =
    Result.failure(BusinessException(msg))

@Suppress("unused")
fun <T> success(value: T): Result<T> =
    Result.success(value)