package cn.edu.buaa.scs.utils

import org.ktorm.entity.EntitySequence
import org.ktorm.entity.find
import org.ktorm.schema.BaseTable
import org.ktorm.schema.ColumnDeclaring

fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.exists(predicate: (T) -> ColumnDeclaring<Boolean>): Boolean =
    find(predicate) != null