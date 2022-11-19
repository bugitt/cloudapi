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

    // not database fields
    fun getVmApply(): VmApply? =
        mysql.vmApplyList.find { it.experimentId.eq(this.id) }

    override fun entityId(): IntOrString {
        return IntOrString(this.id)
    }
}

object Experiments : Table<Experiment>("experiment") {
    @Suppress("unused")
    val id = int("id").primaryKey().bindTo { it.id }

    @Suppress("unused")
    val courseId = int("course_id").references(Courses) { it.course }

    @Suppress("unused")
    val name = varchar("name").bindTo { it.name }

    @Suppress("unused")
    val type = boolean("type").bindTo { it.type }

    @Suppress("unused")
    val detail = text("detail").bindTo { it.detail }

    @Suppress("unused")
    val resource = varchar("resource").bindTo { it.resource }

    @Suppress("unused")
    val createTime = varchar("create_time").bindTo { it.createTime }

    @Suppress("unused")
    val startTime = varchar("start_time").bindTo { it.startTime }

    @Suppress("unused")
    var endTime = varchar("end_time").bindTo { it.endTime }

    @Suppress("unused")
    var deadline = varchar("deadline").bindTo { it.deadline }

    @Suppress("unused")
    var vmStatus = int("vm_status").bindTo { it.vmStatus }

    @Suppress("unused")
    var vmName = varchar("vm_name").bindTo { it.vmName }

    @Suppress("unused")
    val vmApplyId = int("vm_apply_id").bindTo { it.vmApplyId }

    @Suppress("unused")
    val vmPasswd = varchar("vm_passwd").bindTo { it.vmPasswd }

    @Suppress("unused")
    val isPeerAssessment = boolean("is_peer_assessment").bindTo { it.isPeerAssessment }

    @Suppress("unused")
    val peerAssessmentDeadline = varchar("peer_assessment_deadline").bindTo { it.peerAssessmentDeadline }

    @Suppress("unused")
    val appealDeadline = varchar("appeal_deadline").bindTo { it.appealDeadline }

    @Suppress("unused")
    val peerAssessmentRules = text("peer_assessment_rules").bindTo { it.peerAssessmentRules }

    @Suppress("unused")
    val peerAssessmentStart = boolean("peer_assessment_start").bindTo { it.peerAssessmentStart }

    @Suppress("unused")
    val sentEmail = boolean("sent_email").bindTo { it.sentEmail }
}

@Suppress("unused")
val Database.experiments
    get() = this.sequenceOf(Experiments)