package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.int
import org.ktorm.schema.varchar

interface Experiment : Entity<Experiment>, IEntity {
    companion object : Entity.Factory<Experiment>()

    var id: Int
    var courseId: Int
    var name: String
    var type: Boolean
    var detail: String
    var resource: String
    var createTime: String
    var startTime: String
    var endTime: String
    var deadLine: String
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
}

object Experiments : Table<Experiment>("experiment") {
    @Suppress("unused")
    val id = int("id").primaryKey().bindTo { it.id }

    @Suppress("unused")
    val courseId = int("course_id").bindTo { it.courseId }

    @Suppress("unused")
    val name = varchar("name").bindTo { it.name }

    @Suppress("unused")
    val type = boolean("type").bindTo { it.type }

    @Suppress("unused")
    val detail = varchar("detail").bindTo { it.detail }

    @Suppress("unused")
    val resource = varchar("resource").bindTo { it.resource }

    @Suppress("unused")
    val createTime = varchar("create_time").bindTo { it.createTime }

    @Suppress("unused")
    val startTime = varchar("start_time").bindTo { it.startTime }

    @Suppress("unused")
    var endTime = varchar("end_time").bindTo { it.endTime }

    @Suppress("unused")
    var deadline = varchar("deadline").bindTo { it.deadLine }

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
    val peerAssessmentRules = varchar("peer_assessment_rules").bindTo { it.peerAssessmentRules }

    @Suppress("unused")
    val peerAssessmentStart = boolean("peer_assessment_start").bindTo { it.peerAssessmentStart }

    @Suppress("unused")
    val sentEmail = boolean("sent_email").bindTo { it.sentEmail }
}

@Suppress("unused")
val Database.experiments
    get() = this.sequenceOf(Experiments)