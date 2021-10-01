package com.mps.payment.worker.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class HealthController {

    @GetMapping
    fun getOK(): ResponseEntity<*> {
        return ResponseEntity.ok("")
    }
}