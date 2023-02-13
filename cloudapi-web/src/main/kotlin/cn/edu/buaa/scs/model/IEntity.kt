package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.utils.IntOrString

interface IEntity {
    fun entityId(): IntOrString
}