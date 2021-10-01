package com.mps.payment.core.controller

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.model.BranchDTO
import com.mps.payment.core.security.jwt.JwtTokenProvider
import com.mps.payment.core.service.BranchService
import com.mps.payment.core.util.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern

@RestController
@RequestMapping(path = ["branch"])
class BranchController(
    private val branchService: BranchService,
    private val jwtTokenProvider: JwtTokenProvider
) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    fun createBranch(
        @Valid @RequestBody createBranchRequest: CreateBranchRequest, httpRequest: HttpServletRequest
    ): ResponseEntity<*> {
        return try {
            val userName = parseUsername(httpRequest)
            when (val serviceResponse = branchService.createBranch(createBranchRequest, userName)) {
                is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(serviceResponse.obj as BranchDTO)
                is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(serviceResponse.message)
            }
        } catch (e: Exception) {
            log.error("Error creating branch", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error updating branch: ${ExceptionUtils.toString(e)}")
        }
    }

    @PatchMapping
    fun updateBranch(
        @Valid @RequestBody updateBranchRequest: UpdateBranchRequest, httpRequest: HttpServletRequest
    ): ResponseEntity<*> {
        return try {
            val userName = parseUsername(httpRequest)
            when (val serviceResponse = branchService.updateBranch(updateBranchRequest, userName)) {
                is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(serviceResponse.obj as BranchDTO)
                is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(serviceResponse.message)
            }
        } catch (e: Exception) {
            log.error("Error updating branch", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error updating branch : ${ExceptionUtils.toString(e)}")
        }
    }

    @DeleteMapping
    fun deleteBranches(
        @RequestBody ids: List<UUID>, httpRequest: HttpServletRequest
    ): ResponseEntity<*> {
        return try {
            log.info("remove branches: ids=${ids}")
            if (ids.isEmpty()) {
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Empty list")
            } else {
                val userName = parseUsername(httpRequest)
                when (val serviceResponse = branchService.removeBranchesById(ids, userName)) {
                    is GenericResponse.SuccessResponse -> ResponseEntity.ok().body("")
                    is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(serviceResponse.message)
                }
            }
        } catch (e: Exception) {
            log.error("Error deleting products", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error deleting branches ${ExceptionUtils.toString(e)}")
        }
    }

    @GetMapping("/merchant/{merchantId}")
    fun getBranchesByMerchantId(@PathVariable merchantId: UUID, httpRequest: HttpServletRequest): ResponseEntity<*> {
        return try {
            val userName = parseUsername(httpRequest)
            val branches = branchService.findByMerchantId(merchantId, userName)
            if (branches.isEmpty()) {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No branches for this merchant")
            } else {
                ResponseEntity.status(HttpStatus.OK)
                    .body(branches)
            }
        } catch (e: Exception) {
            log.error("Error getting branches", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error getting branches ${ExceptionUtils.toString(e)}")
        }
    }

    private fun parseUsername(request: HttpServletRequest): String {
        val token = request.getHeader("Authorization").replace("Bearer ", "")
        return jwtTokenProvider.getUsername(token)
    }
}

data class CreateBranchRequest(@get:Valid val branch: BranchData, @get:Valid val contact: ContactData)
data class UpdateBranchRequest(val id: UUID, @get:Valid val branch: BranchData, @get:Valid val contact: ContactData)

data class BranchData(
    @get:NotBlank(message = NAME_NOT_BLANK)
    val name: String,
    @get:NotBlank(message = ADDRESS_NOT_BLANK)
    val address: String,
    @get:NotNull(message = DANECODE_NOT_NULL)
    val daneCodeId: UUID
) {
    companion object {
        const val NAME_NOT_BLANK = "Nombre no puede ser nulo o vacío"
        const val ADDRESS_NOT_BLANK = "Dirección no puede ser nula o vacía"
        const val DANECODE_NOT_NULL = "Código DANE no puede ser nulo o vacío"
    }
}

data class ContactData(
    @get:NotBlank(message = NAME_NOT_BLANK)
    val name: String,
    @get:NotNull(message = IDENTIFICATION_NOT_NULL)
    val identification: Long,
    @get:NotNull(message = CONTACT_NOT_BLANK)
    @get:Pattern(regexp = CONTACT_REGEX, message = CONTACT_FORMAT)
    val phone: String,
    @get:NotBlank(message = EMAIL_NOT_BLANK)
    val email: String
) {
    companion object {
        const val NAME_NOT_BLANK = "Nombre no puede ser nulo o vacío"
        const val IDENTIFICATION_NOT_NULL = "Identificación no puede ser nula o vacía"
        const val CONTACT_REGEX = "(^$|[0-9]{10})"
        const val CONTACT_FORMAT = "Número de contacto debe tener diez dígitos"
        const val CONTACT_NOT_BLANK = "Número de contacto no puede ser nulo o vacío"
        const val EMAIL_NOT_BLANK = "Correo electrónico no puede ser nulo o vacío"
    }
}