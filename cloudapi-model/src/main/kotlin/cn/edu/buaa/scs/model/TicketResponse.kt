package cn.edu.buaa.scs.model

/**
 *
 * @param ticket 访问凭证
 * @param host 服务器主机地址
 */
data class TicketResponse(
    /* 访问凭证 */
    val ticket: kotlin.String? = null,
    /* 服务器主机地址 */
    val host: kotlin.String? = null
)
