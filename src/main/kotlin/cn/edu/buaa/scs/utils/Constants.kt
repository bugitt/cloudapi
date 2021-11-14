package cn.edu.buaa.scs.utils

import cn.edu.buaa.scs.model.User
import io.ktor.util.*

const val TOKEN = "token"
val TOKEN_KEY = AttributeKey<String>(TOKEN)

const val USER_KEY_NAME = "user"
val USER_KEY = AttributeKey<User>(USER_KEY_NAME)