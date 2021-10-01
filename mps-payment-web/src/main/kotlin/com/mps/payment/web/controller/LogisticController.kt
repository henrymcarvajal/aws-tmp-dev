package com.mps.payment.web.controller

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.client.entregalo.EntregaloData
import com.mps.payment.core.client.entregalo.payload.CityDTO
import com.mps.payment.core.model.GeneralOrderDrop
import com.mps.payment.core.service.InventoryProcessorService
import com.mps.payment.core.service.LogisticPartnerService
import com.mps.payment.core.service.OrderService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid
import javax.validation.constraints.NotNull

const val ORDER_ID_MANDATORY = "order id es obligatorio"
const val MERCHANT_ID_MANDATORY = "merchant id es obligatorio"

@RestController
@RequestMapping(path = ["logistic"])
class LogisticController(private val logisticService: LogisticPartnerService,
                         private val orderService: OrderService,private val inventoryProcessorService: InventoryProcessorService) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/freight")
    fun requestFreight(@Valid @RequestBody paymentId: String): ResponseEntity<*> {
        log.info("input $paymentId")
        return when (val responseService = logisticService.requestFreightMPS(paymentId, updateFunction)) {
            is GenericResponse.SuccessResponse -> {
                ResponseEntity.ok().body((responseService.obj as EntregaloData).Guia)
            }
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    @PostMapping("/freight/COD")
    fun requestFreightCOD(@Valid @RequestBody request: RequestFreightCODIn): ResponseEntity<*> {
        log.info("input $request")
        return when (val responseService = logisticService.externalRequestFreightCOD(request.orderId, request.sellerMerchantId)) {
            is GenericResponse.SuccessResponse -> {
                val order = responseService.obj as GeneralOrderDrop
                inventoryProcessorService.decreaseInventoryCOD(order.dropShippingSale.product,order)
                ResponseEntity.ok().body(responseService.obj as Int)
            }
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    val updateFunction = { paymentId: String, guide: Int, label:String ->
        orderService.updateGuideToOrder(paymentId, guide,label) }

    @GetMapping("/cities")
    fun getCities(): List<CityDTO>? {
        log.info("getting cities")
        return logisticService.getCities()
    }

    data class RequestFreightCODIn(
            @get:NotNull(message = ORDER_ID_MANDATORY) val orderId: UUID,
            @get:NotNull(message = MERCHANT_ID_MANDATORY) val sellerMerchantId: UUID
    )
}