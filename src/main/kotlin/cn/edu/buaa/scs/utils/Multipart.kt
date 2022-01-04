package cn.edu.buaa.scs.utils

import io.ktor.http.content.*

inline fun <reified T : PartData> getFormItem(partDataList: List<PartData>, name: String = ""): T? {
    return partDataList.find {
        it is T && (name.isEmpty() || name == it.name)
    } as? T
}