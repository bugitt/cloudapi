package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.*

interface TemplateUUID : Entity<TemplateUUID> {
    companion object : Entity.Factory<TemplateUUID>()
    var templateName: String
    var platform: String
    var uuid: String
}

object TemplateUUIDs : Table<TemplateUUID>("template_uuids") {
    val templateName = varchar("template_name").primaryKey().bindTo { it.templateName }
    val platform = varchar("platform").bindTo { it.platform }
    val uuid = varchar("uuid").bindTo { it.uuid }
}

val Database.templateUUIDs get() = this.sequenceOf(TemplateUUIDs)