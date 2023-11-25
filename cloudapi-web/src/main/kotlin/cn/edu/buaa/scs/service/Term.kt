package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.Term
import cn.edu.buaa.scs.model.terms
import cn.edu.buaa.scs.storage.mysql
import io.ktor.server.application.*
import org.ktorm.dsl.eq
import org.ktorm.entity.find
import org.ktorm.entity.first
import org.ktorm.entity.sortedByDescending
import org.ktorm.entity.toList

fun Term.Companion.id(id: Int): Term =
    mysql.terms.find { it.id eq id } ?: throw BusinessException("find term($id) from mysql error")

val ApplicationCall.term
    get() = TermService.getSvc(this) { TermService(this) }

class TermService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<TermService>()

    fun getAllTerms(): List<Term> {
        return mysql.terms.toList().sortedByDescending { it.id }
    }

    fun getLatestTerm(): Term {
        return mysql.terms.sortedByDescending { it.id }.first()
    }
}
