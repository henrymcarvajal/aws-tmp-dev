package com.mps.payment.core.controller

import com.mps.common.dto.ServiceResponse
import com.mps.payment.core.security.jwt.JwtTokenProvider
import com.mps.payment.core.service.InventoryService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

@RestController
@RequestMapping(path = ["inventory"])
class InventoryController(
    private val inventoryService: InventoryService,
    private val jwtTokenProvider: JwtTokenProvider
) {
    companion object {
        const val ERROR_MESSAGE_KEY = "errorMessage"
        const val FINDING_INVENTORY_MESSAGE_ERROR = "Error inesperado en la búsqueda del inventario"
        const val CREATING_INVENTORY_MESSAGE_ERROR = "Error inesperado en la creación del inventario"
        const val UPDATING_INVENTORY_MESSAGE_ERROR = "Error inesperado en la actualización del inventario"
    }

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    fun createInventory(
        @Valid @RequestBody createInventoryRequest: CreateInventoryRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<*> {
        return try {
            val userName = parseUsername(httpRequest)
            when (val serviceResponse = inventoryService.createInventory(createInventoryRequest, userName)) {
                is ServiceResponse.Success -> ResponseEntity.ok().body(serviceResponse.obj as List<*>)
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
    fun updateInventory(
        @Valid @RequestBody updateInventoryRequests: List<@Valid UpdateInventoryRequest>, httpRequest: HttpServletRequest
    ): ResponseEntity<*> {
        return try {
            val userName = parseUsername(httpRequest)
            when (val serviceResponse = inventoryService.updateInventory(updateInventoryRequests, userName)) {
                is ServiceResponse.Success -> ResponseEntity.ok().body(serviceResponse.obj as HashMap<*, *>)
                is ServiceResponse.Error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf(ERROR_MESSAGE_KEY to serviceResponse.message))
            }
        } catch (e: Exception) {
            log.error(UPDATING_INVENTORY_MESSAGE_ERROR, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf(ERROR_MESSAGE_KEY to UPDATING_INVENTORY_MESSAGE_ERROR))
        }
    }

    @GetMapping("/product/{productId}")
    fun findInventoriesByProductId(
        @PathVariable productId: UUID, httpRequest: HttpServletRequest
    ): ResponseEntity<*> {
        return try {
            val userName = parseUsername(httpRequest)
            when (val serviceResponse = inventoryService.findByProductId(productId)) {
                is ServiceResponse.Success -> ResponseEntity.ok().body(serviceResponse.obj as List<*>)
                is ServiceResponse.Error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf(ERROR_MESSAGE_KEY to serviceResponse.message))
            }
        } catch (e: Exception) {
            log.error(UPDATING_INVENTORY_MESSAGE_ERROR, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf(ERROR_MESSAGE_KEY to UPDATING_INVENTORY_MESSAGE_ERROR))
        }
    }

    @GetMapping("/product/geo")
    fun findInventoriesByProductIdGeo(
        @RequestBody getNearestInventoryRequest: GetNearestInventoryRequest, httpRequest: HttpServletRequest
    ): ResponseEntity<*> {
        try {
            val userName = parseUsername(httpRequest)
            val serviceResponse = inventoryService.findNearestByProductId(
                getNearestInventoryRequest.daneCodeId,
                getNearestInventoryRequest.productId,
                getNearestInventoryRequest.quantity
            )
            return when (serviceResponse) {
                is ServiceResponse.Success -> {
                    val searchResult = serviceResponse.obj as Optional<*>
                    if (searchResult.isPresent) {
                        ResponseEntity.ok().body(searchResult.get())
                    } else {
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("")
                    }
                }
                is ServiceResponse.Error -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("")
            }
        } catch (e: Exception) {
            log.error(FINDING_INVENTORY_MESSAGE_ERROR, e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf(ERROR_MESSAGE_KEY to FINDING_INVENTORY_MESSAGE_ERROR))
        }
    }

    private fun parseUsername(request: HttpServletRequest): String {
        val token = request.getHeader("Authorization").replace("Bearer ", "")
        return jwtTokenProvider.getUsername(token)
    }
}

data class CreateInventoryRequest(@get:NotNull(message = PRODUCTID_NOT_NULL) val productId: UUID, @get:Valid val inventories: List<@Valid InventoryItem>) {
    companion object {
        const val PRODUCTID_NOT_NULL = "Id de producto no puede ser vacío"
    }
}

data class InventoryItem(@get:NotNull(message = BRANCHID_NOT_NULL) val branchId: UUID, @get:Min(value = 1, message = MINIMUM_QUANTITY) val quantity: Int) {
    companion object {
        const val BRANCHID_NOT_NULL = "Id de sucursal no puede ser vacío"
        const val MINIMUM_QUANTITY = "cantidad debe ser mínimo 1"
    }
}

data class UpdateInventoryRequest(@get:NotNull(message = PRODUCTID_NOT_NULL) val productId: UUID, @get:NotNull(message = BRANCHID_NOT_NULL) val branchId: UUID,  @get:Min(value = 1, message = MINIMUM_QUANTITY) val quantity: Int) {
    companion object {
        const val PRODUCTID_NOT_NULL = "Id de producto no puede ser vacío"
        const val BRANCHID_NOT_NULL = "Id de branch no puede ser vacío"
        const val MINIMUM_QUANTITY = "cantidad debe ser mínimo 1"
    }
}

data class GetNearestInventoryRequest(val daneCodeId: UUID, val productId: UUID, val quantity: Int)