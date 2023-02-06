package cn.edu.buaa.scs.service

import cn.edu.buaa.scs.auth.assertWrite
import cn.edu.buaa.scs.model.Assignment
import cn.edu.buaa.scs.model.Experiment
import cn.edu.buaa.scs.model.User
import cn.edu.buaa.scs.utils.user
import io.ktor.server.application.*

internal val ApplicationCall.stat get() = StatService.getSvc(this) { StatService(this) }

class StatService(val call: ApplicationCall) : IService {
    companion object : IService.Caller<StatService>()

    fun expAssignments(expId: Int): Map<User, Assignment?> {
        val experiment = Experiment.id(expId)
        call.user().assertWrite(experiment.course)
        val assignments = call.assignment.getAll(expId).associateBy { it.studentId }
        val students = call.course.getAllStudentsInternal(experiment.course.id)
        return students.associateWith { assignments[it.id] }
    }
}
