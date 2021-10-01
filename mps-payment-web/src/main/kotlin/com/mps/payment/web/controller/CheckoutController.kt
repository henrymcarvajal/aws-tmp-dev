package com.mps.payment.web.controller

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.OrderDropDTO
import com.mps.payment.core.enum.PaymentMethod
import com.mps.payment.core.service.OrderService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid


@RestController
@RequestMapping("checkout")
class CheckoutController(private val orderService: OrderService) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/order")
    fun createOrder(@Valid @RequestBody order: OrderDropDTO): ResponseEntity<*> {
        log.info("input $order")
        return when (val responseService = orderService.createOrder(order)) {
            is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(processResponsePerPaymentMethod(order,responseService))
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    private fun processResponsePerPaymentMethod(order: OrderDropDTO, response: GenericResponse.SuccessResponse<*>) =
        if (PaymentMethod.COD.method == order.paymentMethod) {
            response.obj as OrderDropDTO
        } else {
            response.obj as String
        }
}