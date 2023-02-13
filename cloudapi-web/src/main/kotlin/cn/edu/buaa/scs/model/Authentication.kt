package cn.edu.buaa.scs.model

data class Authentication(
    val id: String,
    val token: String,
) : IEntity {
    override fun entityId(): IntOrString {
        return IntOrString(this.id)
    }
}
