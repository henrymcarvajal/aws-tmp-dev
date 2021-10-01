package com.mps.payment.web.controller

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.model.PasswordDTO
import com.mps.payment.core.model.User
import com.mps.payment.core.security.jwt.JwtTokenProvider
import com.mps.payment.core.service.UserService
import io.jsonwebtoken.JwtException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.lang.Exception
import java.util.*
import javax.validation.Valid

@RestController
@RequestMapping("/user")
class UserRestController(
        private val userService: UserService,
        private var jwtTokenProvider: JwtTokenProvider,
        private val passwordEncoder: PasswordEncoder
) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: UUID?): ResponseEntity<User> {
        if (id == null) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val user: User? = userService.findById(id)
        return if (user != null) {
            ResponseEntity(user, HttpStatus.OK)
        } else ResponseEntity(HttpStatus.OK)
    }

    @PostMapping("/integration/{id}")
    fun getIntegrationUserInfo(@PathVariable id: UUID): ResponseEntity<Map<String, String>> {

        return when (val responseService = userService.generateIntegrationInformation(id)) {
            is GenericResponse.SuccessResponse -> ResponseEntity.ok()
                    .body(mapOf("successMessage" to responseService.obj))
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createUser(@RequestBody user: @Valid User): ResponseEntity<User?> {
        if (user.id != null) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val savedUser: User? = userService.create(user)
        return ResponseEntity(savedUser, HttpStatus.OK)
    }

    @PutMapping
    fun updateUser(@RequestBody user: @Valid User): ResponseEntity<User?> {
        if (user.id == null) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val savedUser: User? = userService.update(user)
        return ResponseEntity(savedUser, HttpStatus.OK)
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: UUID?): ResponseEntity<User> {
        if (id == null) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        userService.delete(id)
        return ResponseEntity(HttpStatus.OK)
    }

    @PostMapping("/password")
    fun createPassword(@Valid @RequestBody passwordDTO: PasswordDTO): ResponseEntity<String> {
        try {
            val token = String(Base64.getUrlDecoder().decode(passwordDTO.id))
            val userName = jwtTokenProvider.getUsername(token)
            if (!jwtTokenProvider.validateToken(token)) {
                return ResponseEntity("Token expiró, intenta recuperando contraseña", HttpStatus.UNAUTHORIZED)
            }
            val existingUser: User? = userService.findByUsername(username = userName)
            return if (existingUser != null) {
                if(!existingUser.confirmed){
                    existingUser.confirmed = true
                }
                existingUser.updatePassword(passwordEncoder.encode(passwordDTO.password))
                userService.update(existingUser)
                ResponseEntity("Contraseña creada", HttpStatus.OK)
            } else {
                ResponseEntity("Usuario no existe", HttpStatus.NOT_FOUND)
            }
        }catch (e: JwtException){
            log.error("error creating password token expired",e)
            return ResponseEntity("Token expiró, intenta recuperando contraseña", HttpStatus.UNAUTHORIZED)
        }catch (e: IllegalArgumentException){
            log.error("error creating password token expired",e)
            return ResponseEntity("Token expiró, intenta recuperando contraseña", HttpStatus.UNAUTHORIZED)
        }
    }

    @PostMapping("/recovery-password")
    fun recoveryPassword(@RequestBody email: String): ResponseEntity<String> {
        return try{
            when (val responseService = userService.recoveryPassword(email)) {
                is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as String)
                is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(responseService.objP)
            }
        }catch(e:Exception){
            log.error("error recoveryPassword ${e.localizedMessage}",e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.localizedMessage)
        }
    }
}
