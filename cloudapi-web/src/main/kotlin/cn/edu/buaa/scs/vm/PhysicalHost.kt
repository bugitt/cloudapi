package cn.edu.buaa.scs.vm

data class PhysicalHost (val hostId: String,
                         val cpu_total_mhz: Int,
                         var cpu_used_mhz: Int,
                         var cpu_ratio: Double,
                         val memory_total_mb: Int,
                         var memory_used_mb: Int,
                         var memory_ratio: Double,
                         var hostRefType: String?,
                        var hostRefValue: String?){
}
