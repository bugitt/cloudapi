package cn.edu.buaa.scs.cloudapi.controller

import cn.edu.buaa.scs.cloudapi.model.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/authentications")
class AuthenticationController {

    @GetMapping("")
    fun getUserIdByToken(@RequestParam("token") token: String): List<Authentication> {
        return listOf(Authentication(token, token))
    }
}