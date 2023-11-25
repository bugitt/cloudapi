package cn.edu.buaa.scs.utils

import cn.edu.buaa.scs.model.User
import io.ktor.util.*

const val TOKEN = "token"
val TOKEN_KEY = AttributeKey<String>(TOKEN)

const val USER_KEY_NAME = "user"
val USER_KEY = AttributeKey<User>(USER_KEY_NAME)

const val USER_ID_KEY_NAME = "user_id"
val USER_ID_KEY = AttributeKey<String>(USER_ID_KEY_NAME)

val ERROR_KEY = AttributeKey<Throwable>("error")
