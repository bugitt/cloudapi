@file:Suppress("unused")

package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.IntOrString
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.Entity
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

interface Experiment : Entity<Experiment>, IEntity {
    companion object : Entity.Factory<Experiment>()

    var id: Int
    var course: Course
    var name: String
    var type: Boolean
    var detail: String
    var resource: String?
    var createTime: String
    var startTime: String
    var endTime: String
    var deadline: String
    var vmStatus: Int
    var vmName: String
    var vmApplyId: Int
    var vmPasswd: String
    var isPeerAssessment: Boolean
    var peerAssessmentDeadline: String
    var appealDeadline: String
    var peerAssessmentRules: String
    var peerAssessmentStart: Boolean
    var sentEmail: Boolean
    var enableWorkflow: Boolean

    // not database fields
    fun getVmApply(): VmApply? =
        mysql.vmApplyList.find { it.experimentId.eq(this.id) }

    override fun entityId(): IntOrString {
        return IntOrString(this.id)
    }
}

object Experiments : Table<Experiment>("experiment") {
    val id = int("id").primaryKey().bindTo { it.id }
    val courseId = int("course_id").references(Courses) { it.course }
    val name = varchar("name").bindTo { it.name }
    val type = boolean("type").bindTo { it.type }
    val detail = text("detail").bindTo { it.detail }
    val resource = varchar("resource").bindTo { it.resource }
    val createTime = varchar("create_time").bindTo { it.createTime }
    val startTime = varchar("start_time").bindTo { it.startTime }
    var endTime = varchar("end_time").bindTo { it.endTime }
    var deadline = varchar("deadline").bindTo { it.deadline }
    var vmStatus = int("vm_status").bindTo { it.vmStatus }
    var vmName = varchar("vm_name").bindTo { it.vmName }
    val vmApplyId = int("vm_apply_id").bindTo { it.vmApplyId }
    val vmPasswd = varchar("vm_passwd").bindTo { it.vmPasswd }
    val isPeerAssessment = boolean("is_peer_assessment").bindTo { it.isPeerAssessment }
    val peerAssessmentDeadline = varchar("peer_assessment_deadline").bindTo { it.peerAssessmentDeadline }
    val appealDeadline = varchar("appeal_deadline").bindTo { it.appealDeadline }
    val peerAssessmentRules = text("peer_assessment_rules").bindTo { it.peerAssessmentRules }
    val peerAssessmentStart = boolean("peer_assessment_start").bindTo { it.peerAssessmentStart }
    val sentEmail = boolean("sent_email").bindTo { it.sentEmail }
    val enableWorkflow = boolean("enable_workflow").bindTo { it.enableWorkflow }
}

val Database.experiments
    get() = this.sequenceOf(Experiments)
