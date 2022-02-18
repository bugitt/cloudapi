package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.UserModel
import cn.edu.buaa.scs.model.User

internal fun convertUserModel(user: User): UserModel {
    return UserModel(
        id = user.id,
        name = user.name,
        department = user.departmentId,
        email = user.email,
        role = user.role.name.lowercase(),
    )
}