package cn.edu.buaa.scs.error

class BadRequestException(message: String = "400", cause: Throwable? = null) :
    io.ktor.features.BadRequestException(message, cause)

class AuthenticationException(message: String = "401") : Exception(message)

class AuthorizationException(message: String = "403") : Exception(message)

class NotFoundException(message: String = "404") : Exception(message)