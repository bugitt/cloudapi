package cn.edu.buaa.scs.route

import cn.edu.buaa.scs.controller.models.ChangePasswordRequest
import cn.edu.buaa.scs.controller.models.SimpleUser
import cn.edu.buaa.scs.controller.models.UserModel
import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.model.*
import cn.edu.buaa.scs.service.id
import cn.edu.buaa.scs.service.userService
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

fun Route.userRoute() {
    route("/students") {
        get {
            val search = call.parameters["search"]
            call.respond(call.userService.getStudents(search).map { convertUserModel(it) })
        }
    }

    route("/stuffs") {
        get {
            val search = call.parameters["search"]
            val limit = call.parameters["limit"]?.toIntOrNull() ?: 10
            call.respond(call.userService.getTeachersAndStudents(search, limit).map { convertUserModel(it) })
        }
    }

    route("/myAssistants") {
        get {
            call.respond(call.userService.myAssistants())
        }
    }

    route("/users") {
        route("/{userId}") {
            fun ApplicationCall.getUserIdFromPath(): String =
                parameters["userId"]
                    ?: throw BadRequestException("course id is invalid")

            patch {
                call.userService.patchUser(call.getUserIdFromPath(), call.receive())
                call.respond("OK")
            }

            patch("/changePassword") {
                val req = call.receive<ChangePasswordRequest>()
                call.userService.changePassword(call.getUserIdFromPath(), req.old, req.new)
                call.respond("OK")
            }
        }

    }

    post("/upload") {
        val multipart = call.receiveMultipart()
        var excelFile: InputStream? = null
        var role = UserRole.STUDENT
        multipart.forEachPart { part ->
            when (part) {
                is PartData.FileItem -> {
                    excelFile = part.streamProvider()
                }
                is PartData.FormItem ->
                    when (part.name) {
                        "role" -> role = when (part.value) {
                            "teacher" -> UserRole.TEACHER
                            else -> UserRole.STUDENT
                        }
                    }
                else -> part.dispose()
            }
        }
        if (excelFile != null) {
            val workbook = WorkbookFactory.create(excelFile)
            val sheet = workbook.getSheetAt(0)
            val users = mutableListOf<User>()
            for (rowIndex in sheet.firstRowNum..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex)
                if (rowIndex == sheet.firstRowNum) {
                    continue
                }
                var isValid = true
                val user = User()
                user.role = role
                for (cell in row) {
                    var cellValue = ""
                    when (cell.cellTypeEnum) {
                        CellType.STRING -> {
                            cellValue = cell.stringCellValue
                            println("String Cell Value: $cellValue")
                        }
                        CellType.NUMERIC -> {
                            cellValue = cell.numericCellValue.toLong().toString()
                            println("Numeric Cell Value: $cellValue")
                        }
                        else -> {
                            if (cell.columnIndex in 0..3) isValid = false
                            break
                        }
                    }
                    when (cell.columnIndex) {
                        0 -> {
                            user.id = cellValue
                        }
                        1 -> {
                            user.name = cellValue
                        }
                        2 -> {
                            user.email = cellValue
                        }
                        3 -> {
                            user.departmentId = Department.name(cellValue).id.toInt()
                        }
                    }
                }
                if (isValid) {
                    users.add(user)
                }
            }
            call.userService.batchInsertUser(users)
        }
        call.respond("OK")
    }

    route("/departments") {
        get {
            call.respond(call.userService.getAllDepartments())
        }
    }
}

internal fun convertUserModel(user: User): UserModel {
    return UserModel(
        id = user.id,
        name = user.name,
        department = user.departmentId,
        email = user.email,
        role = user.role.name.lowercase(),
        departmentName = Department.id(user.departmentId).name,
    )
}

internal fun convertSimpleUser(userId: String): SimpleUser? {
    return try {
        if (userId == "admin") return SimpleUser("admin", "管理员")
        val user = User.id(userId)
        SimpleUser(user.id, user.name)
    } catch (e: Throwable) {
        null
    }
}
