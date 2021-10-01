package com.mps.payment.web.controller

import com.mps.common.dto.ServiceResponse
import com.mps.payment.core.controller.CreatePrivateInventoryRequest
import com.mps.payment.core.controller.UpdatePrivateInventoryRequest
import com.mps.payment.core.model.PrivateInventoryDTO
import com.mps.payment.core.model.PrivateInventoryView
import com.mps.payment.core.model.Product
import com.mps.payment.core.model.toDTO
import com.mps.payment.core.security.jwt.JwtTokenProvider
import com.mps.payment.core.service.PrivateInventoryService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@RestController
@RequestMapping(path = ["privateinventory"])
class PrivateInventoryController(
        private val privateInventoryService: PrivateInventoryService,
        private val jwtTokenProvider: JwtTokenProvider
) {
    companion object {
        const val ERROR_MESSAGE_KEY = "errorMessage"
        const val CREATING_INVENTORY_MESSAGE_ERROR = "Error inesperado en la creación del inventario privado"
        const val UPDATING_INVENTORY_MESSAGE_ERROR = "Error inesperado en la actualización del inventario privado"
        const val DELETING_INVENTORY_MESSAGE_ERROR = "Error inesperado en el borrado del inventario privado"
    }

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    fun createPrivateInventory(
        @Valid @RequestBody createPrivateInventoryRequest: CreatePrivateInventoryRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<*> {
        return try {
            val userName = parseUsername(httpRequest)
            when (val serviceResponse = privateInventoryService.createPrivateInventory(createPrivateInventoryRequest, userName)) {
                is ServiceResponse.Success -> ResponseEntity.ok().body(serviceResponse.obj as PrivateInventoryDTO)
                is ServiceResponse.Error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(mapOf(ERROR_MESSAGE_KEY to serviceResponse.message))
            }
        } catch (e: Exception) {
            log.error(CREATING_INVENTORY_MESSAGE_ERROR, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf(ERROR_MESSAGE_KEY to CREATING_INVENTORY_MESSAGE_ERROR))
        }
    }

    @PatchMapping
    fun updatePrivateInventory(
        @Valid @RequestBody updatePrivateInventoryRequest: UpdatePrivateInventoryRequest, httpRequest: HttpServletRequest
    ): ResponseEntity<*> {
        return try {
            val userName = parseUsername(httpRequest)
            when (val serviceResponse = privateInventoryService.updatePrivateInventory(updatePrivateInventoryRequest, userName)) {
                is ServiceResponse.Success -> ResponseEntity.ok().body(serviceResponse.obj as PrivateInventoryDTO)
                is ServiceResponse.Error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(mapOf(ERROR_MESSAGE_KEY to serviceResponse.message))
            }
        } catch (e: Exception) {
            log.error(UPDATING_INVENTORY_MESSAGE_ERROR, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf(ERROR_MESSAGE_KEY to UPDATING_INVENTORY_MESSAGE_ERROR))
        }
    }

    @DeleteMapping("/{privateInventoryId}")
    fun deletePrivateInventory(
            @PathVariable privateInventoryId: UUID,
            httpRequest: HttpServletRequest
    ): ResponseEntity<*> {
        return try {
            val userName = parseUsername(httpRequest)
            when (val serviceResponse = privateInventoryService.deletePrivateInventory(privateInventoryId, userName)) {
                is ServiceResponse.Success -> ResponseEntity.ok().body(serviceResponse.obj as HashMap<*, *>)
                is ServiceResponse.Error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(mapOf(ERROR_MESSAGE_KEY to serviceResponse.message))
            }
        } catch (e: Exception) {
            log.error(DELETING_INVENTORY_MESSAGE_ERROR, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf(ERROR_MESSAGE_KEY to DELETING_INVENTORY_MESSAGE_ERROR))
        }
    }

    @GetMapping("/{privateInventoryId}")
    fun findById(
            @PathVariable privateInventoryId: UUID, httpRequest: HttpServletRequest
    ): ResponseEntity<*> {
        return try {
            val userName = parseUsername(httpRequest)
            val serviceResponse = privateInventoryService.findById(privateInventoryId)
            when (serviceResponse.isPresent) {
                true -> ResponseEntity.ok().body(serviceResponse.get() as PrivateInventoryDTO)
                false -> ResponseEntity.status(HttpStatus.NOT_FOUND).build()
            }
        } catch (e: Exception) {
            log.error("Error updating inventories", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf(ERROR_MESSAGE_KEY to "Unexpected error updating inventories"))
        }
    }

    @GetMapping("/product/{merchantId}")
    fun findPrivateInventory(
            @PathVariable merchantId: UUID, httpRequest: HttpServletRequest
    ): ResponseEntity<*> {
        return try {
            val products = privateInventoryService.findProductsByPrivateInventory(merchantId)
            if (products.isEmpty()) {
                ResponseEntity.notFound().build()
            } else {
                ResponseEntity.status(HttpStatus.OK)
                        .body((products as List<Product>).map { it.toDTO() })
            }
        } catch (e: Exception) {
            log.error("Error getting private products", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf(ERROR_MESSAGE_KEY to "Error getting private products"))
        }
    }

    @GetMapping("/provider/{providerId}")
    fun findPrivateInventoryByProvider(
            @PathVariable providerId: UUID, httpRequest: HttpServletRequest
    ): ResponseEntity<*> {
        return try {
            val products = privateInventoryService.findPrivateInventoryByProvider(providerId)
            if (products.isEmpty()) {
                ResponseEntity.notFound().build()
            } else {
                ResponseEntity.status(HttpStatus.OK)
                        .body((products as List<PrivateInventoryView>))
            }
        } catch (e: Exception) {
            log.error("Error getting private products", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf(ERROR_MESSAGE_KEY to "Error getting private products"))
        }
    }

    private fun parseUsername(request: HttpServletRequest): String {
        val token = request.getHeader("Authorization").replace("Bearer ", "")
        return jwtTokenProvider.getUsername(token)
    }
}
