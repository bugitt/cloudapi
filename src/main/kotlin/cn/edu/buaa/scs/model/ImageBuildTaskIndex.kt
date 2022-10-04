@file:Suppress("unused")

package cn.edu.buaa.scs.model

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.long
import org.ktorm.schema.varchar

interface ImageBuildTaskIndex : Entity<ImageBuildTaskIndex> {
    companion object : Entity.Factory<ImageBuildTaskIndex>() {
        fun buildFromImageMeta(projectId: Long, imageMeta: ImageMeta, taskDataId: Long = 0) = ImageBuildTaskIndex {
            this.projectId = projectId
            this.owner = imageMeta.owner
            this.repo = imageMeta.repo
            this.tag = imageMeta.tag
            this.taskDataId = taskDataId
        }
    }

    var id: Long
    var projectId: Long
    var owner: String
    var repo: String
    var tag: String
    var taskDataId: Long
}

object ImageBuildTaskIndexList : Table<ImageBuildTaskIndex>("image_build_task_index") {
    val id = long("id").primaryKey().bindTo { it.id }
    val projectId = long("project_id").bindTo { it.projectId }
    val owner = varchar("owner").bindTo { it.owner }
    val repo = varchar("repo").bindTo { it.repo }
    val tag = varchar("tag").bindTo { it.tag }
    val taskDataId = long("task_data_id").bindTo { it.taskDataId }
}

val Database.imageBuildTaskIndexList get() = this.sequenceOf(ImageBuildTaskIndexList)