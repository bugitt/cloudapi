package cn.edu.buaa.scs.model

data class Host(
    val ip: String,
    val status: String,
    val totalMem: Double,
    val usedMem: Double,
    val totalCPU: Double,
    val usedCPU: Double,
    val totalStorage: Long,
    val usedStorage: Long,
    val count: Int,
)
