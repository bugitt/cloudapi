package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.controller.models.TermModel
import cn.edu.buaa.scs.error.BusinessException
import cn.edu.buaa.scs.model.Courses
import cn.edu.buaa.scs.model.Experiments
import cn.edu.buaa.scs.model.Term
import cn.edu.buaa.scs.model.terms
import cn.edu.buaa.scs.storage.mysql
import io.ktor.server.application.*
import org.ktorm.database.asIterable
import org.ktorm.dsl.*
import org.ktorm.entity.*

fun Term.Companion.id(id: Int): Term =
    mysql.terms.find { it.id eq id } ?: throw BusinessException("find term($id) from mysql error")

val ApplicationCall.term
    get() = TermService.getSvc(this) { TermService(this) }

class TermService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<TermService>()

    fun getAllTerms(): List<TermModel> {
        val terms = mysql.useConnection { conn ->
            val sql = """
                select term.id, term.name, count(1), sum(c.exp_cnt)
                from term left join (
                    select c.id, c.term_id, count(1) exp_cnt
                    from course c left join experiment e on c.id = e.course_id
                    group by c.id
                ) c on c.term_id=term.id
                group by term.id
                order by term.id desc
            """.trimIndent()
            conn.prepareStatement(sql).use { statement ->
                statement.executeQuery().asIterable().map { TermModel(
                    id = it.getInt(1),
                    name = it.getString(2),
                    courseCount = it.getInt(3),
                    expCount = it.getInt(4),
                ) }
            }
        }
        return terms
    }

    fun getLatestTerm(): Term {
        return mysql.terms.sortedByDescending { it.id }.first()
    }

    fun createTerm(name: String) {
        val term = Term {
            this.name = name
        }
        mysql.terms.add(term)
    }

    fun deleteTerm(id: Int) {
        val term = mysql.terms.find { it.id.eq(id) } ?: return
        term.delete()
    }
}
