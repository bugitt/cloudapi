package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.TermModel
import cn.edu.buaa.scs.model.Term

fun convertTermModel(term: Term): TermModel {
    return TermModel(term.id, term.name)
}