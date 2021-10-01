package com.mps.payment.pull.col.controller

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.PaymentDTO
import com.mps.payment.pull.col.model.RequestWoocommerce
import com.mps.payment.pull.col.service.WooService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping(path = ["woo"])
class WooController(private val wooService: WooService) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    fun wooPost(@RequestBody request: RequestWoocommerce): ResponseEntity<*> {
        log.info("woo post executed $request")
           return when (val responseService = wooService.processPaymentFromCheckout(request)) {
                is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as String)
                is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error creating payment")
            }
    }
}