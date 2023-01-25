package cn.edu.buaa.scs.service

class PaginationResponse<T>(
    val data: List<T>,
    val total: Long,
    val page: Int,
    val success: Boolean = true,
)
