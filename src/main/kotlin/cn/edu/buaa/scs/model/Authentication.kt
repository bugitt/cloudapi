package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.utils.IntOrString

data class Authentication(
    val id: String,
    val token: String,
) : IEntity {
    override fun entityId(): IntOrString {
        return IntOrString(this.id)
    }
}