package com.mps.payment.pull.col.controller

import com.mps.payment.pull.col.service.CashInService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern


@RestController
@RequestMapping(path = ["cashin"])
class CashInController(private val cashInService: CashInService) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/redirect")
    fun generateRedirect(@Valid @RequestBody generateRedirectRequest: GenerateRedirectRequest): ResponseEntity<*> {
        return try {
            val paymentId = generateRedirectRequest.id
            log.info("generating redirect $paymentId")
            cashInService.createCashInRedirect(paymentId)?.let {
                ResponseEntity.ok(it)
            } ?: let {
                log.error("There was a problem generating redirect")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("There was a problem generating redirect")
            }
        } catch (e: Exception) {
            log.error("there was an error generating redirect", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("There was a problem generating redirect")
        }
    }

    @PatchMapping("/redirect/paymentmethod")
    fun updatePaymentMethod(@Valid @RequestBody request: UpdatePaymentMethodRequest): ResponseEntity<*> {
        return try {
            cashInService.setPaymentMethodAndGenerateSignature(request.paymentMethod, request.paymentPartnerId)?.let {
                ResponseEntity.ok(it)
            } ?: let {
                log.error("There was a problem updating payment method")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("There was a problem updating payment method")
            }
        } catch (e: Exception) {
            log.error("there was an error updating payment method", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("There was a problem generating redirect")
        }
    }

    @PostMapping("/redirect/notification", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun notification(@RequestParam params: HashMap<String, String>) {
        try {
            log.info("receiving notification $params")
            if (cashInService.processNotification(params)) {
                log.info("payment updated Successfully")
            } else {
                log.error("there was a problem processing notification")
            }
        } catch (e: Exception) {
            log.error("there was an error notifing error", e)
        }
    }
}

data class GenerateRedirectRequest(@get:NotNull(message = "id is mandatory") val id: String)
data class UpdatePaymentMethodRequest(@get:NotNull(message = "paymentPartnerId is mandatory") val paymentPartnerId: String,
                                      @get:Pattern(regexp = "EFECTY|PSE|MASTERCARD|VISA", message = "Medio de pago invalido") val paymentMethod: String)