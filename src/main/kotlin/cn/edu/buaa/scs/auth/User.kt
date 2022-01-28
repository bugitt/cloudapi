package cn.edu.buaa.scs.auth

import cn.edu.buaa.scs.error.AuthorizationException
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.service.getCourse
import cn.edu.buaa.scs.service.id

fun User.authRead(entity: IEntity): Boolean {
    if (isAdmin()) return true

    return when (entity) {
        is Course ->
            isCourseStudent(entity) || isCourseAssistant(entity) || isCourseTeacher(entity)

        is Experiment ->
            authRead(entity.getCourse())

        is Assignment ->
            // 学生本人
            entity.studentId == this.id
                    // 或这门课的老师、助教
                    || entity.getCourse().let { isCourseAssistant(it) || isCourseTeacher(it) }

        is File ->
            when (entity.fileType) {
                FileType.Assignment -> authRead(Assignment.id(entity.involvedId))
            }

        else -> throw BadRequestException("unsupported auth entity: $entity")
    }
}

fun User.assertRead(entity: IEntity) {
    if (!this.authRead(entity)) {
        throw AuthorizationException("${this.id} has no read access to $entity")
    }
}

fun User.authWrite(entity: IEntity): Boolean {
    if (isAdmin()) return true

    return when (entity) {
        is Course ->
            isCourseAssistant(entity) || isCourseTeacher(entity)

        is Experiment ->
            authWrite(entity.getCourse())

        is Assignment ->
            // 作业的读写权限是一样的
            authRead(entity)

        is File ->
            when (entity.fileType) {
                FileType.Assignment -> authWrite(Assignment.id(entity.involvedId))
            }

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
            entity.getCourse().let {
                isCourseTeacher(it) || isCourseStudent(it)
            }

        is Assignment ->
            // 作业的读写权限是一样的
            authRead(entity)

        is File ->
            when (entity.fileType) {
                FileType.Assignment -> authAdmin(Assignment.id(entity.involvedId))
            }

        else -> throw BadRequestException("unsupported auth entity: $entity")
    }
}

fun User.assertAdmin(entity: IEntity) {
    if (!this.authAdmin(entity)) {
        throw AuthorizationException("${this.id} has no admin access to $entity")
    }
}