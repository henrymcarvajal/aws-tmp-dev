package com.mps.payment.core.controller

import com.mps.common.auth.request.AuthenticationRequest
import com.mps.common.dto.GenericResponse
import com.mps.payment.core.model.*
import com.mps.payment.core.service.AuthService
import com.mps.payment.core.service.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(
        private val authService: AuthService,
        private val userService: UserService

) {
    val log: Logger = LoggerFactory.getLogger(this::class.java)


    @PostMapping("/signin")
    fun signin(@RequestBody request: AuthenticationRequest): ResponseEntity<*> {
        log.info("input signin $request")
        return when (val serviceResponse = authService.signin(request.username, request.password)) {
            is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(serviceResponse.obj as MutableMap<String, String>)
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(mapOf("errorMessage" to serviceResponse.message))
        }
    }

    @PostMapping("/signup")
    fun signup(@RequestBody userDto: UserDto): ResponseEntity<*> {
        val createdUser: User? = userService.create(userDto.toEntity())
        val newDto = createdUser!!.toDto()
        return ResponseEntity(newDto, HttpStatus.OK)
    }

}