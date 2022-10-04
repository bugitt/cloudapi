package cn.edu.buaa.scs.model

data class ImageMeta(
    val owner: String,
    val repo: String,
    val tag: String,
) {
    companion object {
        const val hostPrefix = "scs.buaa.edu.cn:8081"
    }

    fun uri() = "$hostPrefix/${owner}/${repo}:${tag}"
}
