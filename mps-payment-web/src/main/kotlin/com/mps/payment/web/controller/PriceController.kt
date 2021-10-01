package com.mps.payment.web.controller

import com.mps.payment.core.service.PriceService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping(path = ["price"])
class PriceController(
    private val priceService: PriceService
) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/{quantity}/{basePrice}")
    fun getPrice(@RequestBody conditions: String, @PathVariable quantity: Int, @PathVariable basePrice: Int): ResponseEntity<*> {
        val response = priceService.getOrderPrice(UUID.randomUUID(), quantity, basePrice.toBigDecimal(), conditions)
        return ResponseEntity.ok().body(response)
    }
}