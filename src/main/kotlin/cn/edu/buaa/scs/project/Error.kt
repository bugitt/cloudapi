package cn.edu.buaa.scs.project

import cn.edu.buaa.scs.error.BadRequestException

class ProjectNameDuplicateException(projectName: String) :
    BadRequestException("project name $projectName already exists")

class ProjectNameInvalidException(projectName: String) :
    BadRequestException("project name $projectName is invalid")