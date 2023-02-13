package cn.edu.buaa.scs.utils

object Constants {
    object VCenter {
        val port = (System.getenv("VCENTER_PORT") ?: "9977").toInt()
        val endpoint = System.getenv("VCENTER_ENDPOINT") ?: "http://localhost:9977"
        val username = System.getenv("VCENTER_USERNAME") ?: ""
        val password = System.getenv("VCENTER_PASSWORD") ?: ""
    }
}
