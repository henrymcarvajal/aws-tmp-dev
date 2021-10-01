package com.mps.payment.core.controller

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.model.DropshippingSaleDTO
import com.mps.payment.core.model.OrderConsolidateGroupByStatus
import com.mps.payment.core.model.UpdateAmountDropSale
import com.mps.payment.core.service.DropshippingSaleService
import com.mps.payment.core.util.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid


@RestController
@RequestMapping(path = ["dropshippingsale"])
class DropshippingSaleController(private val dropshippingSaleService: DropshippingSaleService) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    fun createDropshippingSale(request: HttpServletRequest,@Valid @RequestBody dropshippingSaleDTO: DropshippingSaleDTO): ResponseEntity<*> {

        return try {
            val token = request.getHeader("Authorization").replace("Bearer ","")
            when (val serviceResponse = dropshippingSaleService.createDropshippingSale(dropshippingSaleDTO,token)) {
                is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(serviceResponse.obj as DropshippingSaleDTO)
                is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to serviceResponse.message))
            }
        } catch (e: Exception) {
            log.error("Error creating dropshipping sale", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error creating dropshipping sale ${ExceptionUtils.toString(e)}")
        }
    }

    @PutMapping
    fun updateDropshippingSale(@Valid @RequestBody dropshippingSaleDTO: DropshippingSaleDTO): ResponseEntity<*> {
        if (dropshippingSaleDTO.id == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("errorMessage" to "id es nulo"))
        return try {
            when (val serviceResponse = dropshippingSaleService.updateDropshippingSale(dropshippingSaleDTO)) {
                is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(serviceResponse.obj as DropshippingSaleDTO)
                is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to serviceResponse.message))
            }
        } catch (e: Exception) {
            log.error("Error updating dropshipping sale", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error creating dropshipping sale ${ExceptionUtils.toString(e)}")
        }
    }
    @PatchMapping
    fun updateAmountDropshippingSale(@Valid @RequestBody updateAmountRequest: UpdateAmountDropSale): ResponseEntity<*> {
        return try {
            when (val responseService = dropshippingSaleService.updateAmountDropshippingSale(updateAmountRequest)) {
                is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as DropshippingSaleDTO)
                is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(mapOf("errorMessage" to responseService.message))
            }
        } catch (e: Exception) {
            log.error("Error updating amount dropshipping sale", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error creating dropshipping sale")
        }
    }


    @DeleteMapping
    fun removeDropshippingSales(@RequestBody ids: List<String>): ResponseEntity<*> {
        return try {
            //val ids = request.ids
            log.info("disabling dropshipping sales: $ids")
            if (ids.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(mapOf("errorMessage" to "empty list"))
            }
            when (val serviceResponse = dropshippingSaleService.removeDropshippingSalesById(ids)) {
                is GenericResponse.SuccessResponse<*> -> ResponseEntity.ok().body(serviceResponse.obj as String)
                is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(mapOf("errorMessage" to serviceResponse.message))
            }
        } catch (e: Exception) {
            log.error("Error disabling dropshipping items", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error deleting product ${ExceptionUtils.toString(e)}")
        }
    }

    @GetMapping("/public/view/checkout/{id}")
    fun getDropshippingSaleCheckoutViewByShortId(@PathVariable id: UUID): ResponseEntity<*> {
        return try {
            val dropshippingSale = dropshippingSaleService.getDropshippingSaleCheckoutViewByShortId(id)
            if (dropshippingSale == null) {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null)
            } else {
                ResponseEntity.status(HttpStatus.OK)
                    .body(dropshippingSale)
            }
        } catch (e: Exception) {
            log.error("Error getting dropshipping sales", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error getting dropshipping sales ${ExceptionUtils.toString(e)}")
        }
    }

    @GetMapping("/results/dropshippers")
    fun getOrderConsolidateGrupByStatus(@RequestParam("merchantId") merchantId: UUID,
                                        @RequestParam("initialDate")
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  initialDate: LocalDate,
                                        @RequestParam("finalDate")
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) finalDate: LocalDate): ResponseEntity<*> {
        return try {

            return when (val response = dropshippingSaleService.getResultForDropshipper(merchantId,
                    initialDate,finalDate)) {
                is GenericResponse.SuccessResponse -> {
                    val orders = response.obj as List<OrderConsolidateGroupByStatus>
                    return if (orders.isEmpty()) {
                        ResponseEntity.notFound().build<OrderConsolidateGroupByStatus>()
                    } else {
                        ResponseEntity.ok().body(orders)
                    }
                }
                else -> {
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response.objP as String)
                }
            }

        } catch (e: Exception) {
            log.error("Error getOrderConsolidateGrupByStatus", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error getting dropshipping sales ${ExceptionUtils.toString(e)}")
        }
    }

    @GetMapping("/sellermerchant/{sellerMerchantId}")
    fun getDropshippingSalesByMerchant(@PathVariable sellerMerchantId: UUID): ResponseEntity<*> {
        return try {
            val products = dropshippingSaleService.getDropshippingSaleBySellerMerchantId(sellerMerchantId)
            if (products == null) {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null)
            } else {
                ResponseEntity.status(HttpStatus.OK)
                    .body(products)
            }
        } catch (e: Exception) {
            log.error("Error getting dropshipping sales", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error getting dropshipping sales ${ExceptionUtils.toString(e)}")
        }
    }
}
