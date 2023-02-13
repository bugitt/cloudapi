package cn.edu.buaa.scs.utils

object CommonUtil {
    fun isEmpty(vararg objs: Any?): Boolean {
        fun work(obj: Any?): Boolean {
            if (obj == null) {
                return false
            }
            return when (obj) {
                is String -> obj.isEmpty()
                is Collection<*> -> obj.isEmpty()
                is Array<*> -> obj.isEmpty()
                else -> return true
            }
        }
        return !objs.all { !work(it) }
    }
}