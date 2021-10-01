package com.mps.payment.worker.controller

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping
class QueueController {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    fun onSqsMessage(
        @RequestBody sqsMessage: String
    ): ResponseEntity<*> {
        log.info(sqsMessage)
        return ResponseEntity.ok("")
    }
}