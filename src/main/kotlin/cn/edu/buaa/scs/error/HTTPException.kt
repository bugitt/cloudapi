package cn.edu.buaa.scs.error

@Suppress("unused")
class BadRequestException(message: String = "400") : io.ktor.features.BadRequestException(message)

class AuthenticationException(message: String = "401") : Exception(message)

class AuthorizationException(message: String = "403") : Exception(message)

class NotFoundException(message: String = "404") : Exception(message)