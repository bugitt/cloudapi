package cn.edu.buaa.scs.auth

import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.service.course
import cn.edu.buaa.scs.service.id
import cn.edu.buaa.scs.service.isPeerTarget
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.exists
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.map

fun User.hasAccessToStudent(studentId: String): Boolean {
    if (this.id == studentId) return true
    if (isAdmin()) return true
    return mysql.courseStudents.filter { it.studentId.eq(this.id) }.map { it.courseId }.distinct()
        .any { this.isCourseTeacher(it) || this.isCourseAssistant(it) }
}

fun User.authRead(entity: IEntity): Boolean {
    if (isAdmin()) return true

    return when (entity) {
        is Course ->
            isCourseStudent(entity) || isCourseAssistant(entity) || isCourseTeacher(entity)

        is Experiment ->
            authRead(entity.course)

        is Assignment ->
            // 学生本人
            entity.studentId == this.id
                    // 或这门课的老师、助教
                    || Course.id(entity.courseId).let { isCourseAssistant(it) || isCourseTeacher(it) }
                    || Assignment.isPeerTarget(entity.id, this.id)

        is CourseResource ->
            authRead(entity.course)

        is File ->
            when (entity.fileType) {
                FileType.Assignment -> authRead(Assignment.id(entity.involvedId))
                FileType.CourseResource -> authRead(Course.id(entity.involvedId))
                FileType.ExperimentResource -> authRead(Experiment.id(entity.involvedId))
                FileType.AssignmentReview -> authRead(Assignment.id(entity.involvedId))
                FileType.ImageBuildContextTar -> authRead(Project.id(entity.involvedId.toLong()))
            }

        is PeerAppeal ->
            entity.studentId == this.id
                    || authWrite(Experiment.id(entity.expId))

        is VirtualMachine ->
            entity.studentId == this.id // 学生自己
                    || entity.teacherId == this.id // 实验虚拟机的任课老师
                    || entity.experimentId != 0 && authWrite(Experiment.id(entity.experimentId)) // 对实验虚拟机所属的实验有写权限的人，包括教师和助教

        is Project ->
            mysql.projectMembers.exists { it.projectId.eq(entity.id) and it.userId.eq(this.id) }
                    || isProjectAdmin(entity)

        else -> throw BadRequestException("unsupported auth entity: $entity")
    }
}

fun User.assertRead(entity: IEntity) {
    if (!this.authRead(entity)) {
        throw AuthorizationException("${this.id}没有权限访问$entity")
    }
}

fun User.authWrite(entity: IEntity): Boolean {
    if (isAdmin()) return true

    return when (entity) {
        is Course ->
            isCourseAssistant(entity) || isCourseTeacher(entity)

        is Experiment ->
            authWrite(entity.course)

        is Assignment ->
            // 作业的读写权限是一样的
            authRead(entity)

        is CourseResource ->
            authWrite(entity.course)

        is File ->
            when (entity.fileType) {
                FileType.Assignment -> authWrite(Assignment.id(entity.involvedId))
                FileType.CourseResource -> authWrite(Course.id(entity.involvedId))
                FileType.ExperimentResource -> authWrite(Experiment.id(entity.involvedId))
                FileType.AssignmentReview -> authWrite(Experiment.id(Assignment.id(entity.involvedId).experimentId))
                FileType.ImageBuildContextTar -> authWrite(Project.id(entity.involvedId.toLong()))
            }

        is PeerAppeal ->
            entity.studentId == this.id
                    || authWrite(Experiment.id(entity.expId))

        is VirtualMachine ->
            authRead(entity)

        is VmApply ->
            Experiment.id(entity.experimentId).course.let {
                isCourseAdmin(it)
            }

        is Project ->
            authRead(entity)

        else -> throw BadRequestException("unsupported auth entity: $entity")
    }
}

fun User.assertWrite(entity: IEntity) {
    if (!this.authWrite(entity)) {
        throw AuthorizationException("${this.id} has no write access to $entity")
    }
}

fun User.authAdmin(entity: IEntity): Boolean {
    if (isAdmin()) return true

    return when (entity) {
        is Course ->
            isCourseTeacher(entity)

        is Experiment ->
            // 这门课的助教或老师可以新建或删除实验
            entity.course.let {
                isCourseTeacher(it) || isCourseStudent(it)
            }

        is Assignment ->
            // 作业的读写权限是一样的
            authRead(entity)

        is CourseResource ->
            authAdmin(entity.course)

        is File ->
            when (entity.fileType) {
                FileType.Assignment -> authWrite(Assignment.id(entity.involvedId))
                FileType.CourseResource -> authWrite(Course.id(entity.involvedId))
                FileType.ExperimentResource -> authWrite(Experiment.id(entity.involvedId))
                FileType.AssignmentReview -> authWrite(Experiment.id(Assignment.id(entity.involvedId).experimentId))
                FileType.ImageBuildContextTar -> authWrite(Project.id(entity.involvedId.toLong()))
            }

        is PeerAppeal ->
            entity.studentId == this.id
                    || authWrite(Experiment.id(entity.expId))

        else -> throw BadRequestException("unsupported auth entity: $entity")
    }
}

fun User.assertAdmin(entity: IEntity) {
    if (!this.authAdmin(entity)) {
        throw AuthorizationException("${this.id} has no admin access to $entity")
    }
}