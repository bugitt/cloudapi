@file:Suppress("unused")

package cn.edu.buaa.scs.model

import cn.edu.buaa.scs.service.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

sealed interface FileType {
    val name: String
    fun getInvolvedEntity(involvedId: Int): IEntity

    fun decorator(call: ApplicationCall): FileService.FileDecorator

    companion object {
        fun valueOf(name: String): FileType {
            return when (name.lowercase()) {
                "Assignment".lowercase() -> Assignment
                "CourseResource".lowercase() -> CourseResource
                "ExperimentResource".lowercase() -> ExperimentResource
                "AssignmentReview".lowercase() -> AssignmentReview
                "ImageBuildContextTar".lowercase() -> ImageBuildContextTar
                "ExperimentWorkflowContext".lowercase() -> ExperimentWorkflowContext
                else -> throw BadRequestException("Unknown file type: $name")
            }
        }
    }

    object Assignment : FileType {
        override val name: String
            get() = "Assignment"

        override fun getInvolvedEntity(involvedId: Int): IEntity {
            return cn.edu.buaa.scs.model.Assignment.id(involvedId)
        }

        override fun decorator(call: ApplicationCall): FileService.FileDecorator {
            return call.assignment
        }
    }

    sealed interface Resource : FileType {
        companion object {
            fun valueOf(name: String): Resource {
                return when (name) {
                    "CourseResource" -> CourseResource
                    "ExperimentResource" -> ExperimentResource
                    else -> throw BadRequestException("Unknown resource type: $name")
                }
            }
        }
    }

    object CourseResource : Resource {
        override val name: String
            get() = "CourseResource"

        override fun getInvolvedEntity(involvedId: Int): IEntity {
            return Course.id(involvedId)
        }

        override fun decorator(call: ApplicationCall): FileService.FileDecorator {
            return call.courseResource
        }
    }

    object ExperimentResource : Resource {
        override val name: String
            get() = "ExperimentResource"

        override fun getInvolvedEntity(involvedId: Int): IEntity {
            return Experiment.id(involvedId)
        }

        override fun decorator(call: ApplicationCall): FileService.FileDecorator {
            return call.experiment
        }
    }

    object AssignmentReview : FileType {
        override val name: String
            get() = "AssignmentReview"

        override fun getInvolvedEntity(involvedId: Int): IEntity {
            return cn.edu.buaa.scs.model.Assignment.id(involvedId)
        }

        override fun decorator(call: ApplicationCall): FileService.FileDecorator {
            return call.assignmentReview
        }
    }

    object ImageBuildContextTar : FileType {
        override val name: String
            get() = "ImageBuildContextTar"

        override fun getInvolvedEntity(involvedId: Int): IEntity {
            return Project.id(involvedId.toLong())
        }

        override fun decorator(call: ApplicationCall): FileService.FileDecorator {
            return call.project
        }
    }

    object ExperimentWorkflowContext : FileType {
        override val name: String
            get() = "ExperimentWorkflowContext"

        override fun getInvolvedEntity(involvedId: Int): IEntity {
            return Experiment.id(involvedId)
        }

        override fun decorator(call: ApplicationCall): FileService.FileDecorator {
            return ExperimentWorkflowContextFileDecorator
        }
    }
}

interface File : Entity<File>, IEntity {
    companion object : Entity.Factory<File>()

    var id: Int
    var name: String
    var storeType: String
    var storeName: String
    var storePath: String
    var uploadTime: Long
    var fileType: FileType
    var involvedId: Int
    var size: Long
    var uploader: String
    var contentType: String
    var owner: String

    var createdAt: Long
    var updatedAt: Long

    override fun entityId(): IntOrString {
        return IntOrString(this.id)
    }
}

object Files : Table<File>("file_v2") {
    val id = int("id").primaryKey().bindTo { it.id }

    val name = varchar("name").bindTo { it.name }

    val storeType = varchar("store_type").bindTo { it.storeType }

    val storeName = varchar("store_name").bindTo { it.storeName }

    val storePath = varchar("store_path").bindTo { it.storePath }

    val uploadTime = long("upload_time").bindTo { it.uploadTime }

    val fileType = varchar("file_type").transform({ FileType.valueOf(it) }, { it.name }).bindTo { it.fileType }

    val involvedId = int("involved_id").bindTo { it.involvedId }

    val size = long("size").bindTo { it.size }

    val contentType = varchar("content_type").bindTo { it.contentType }

    val uploader = varchar("uploader").bindTo { it.uploader }

    val owner = varchar("owner").bindTo { it.owner }

    val createdAt = long("created_at").bindTo { it.createdAt }

    val updatedAt = long("updated_at").bindTo { it.updatedAt }
}

val Database.files get() = this.sequenceOf(Files)
