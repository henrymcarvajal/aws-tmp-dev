package com.mps.payment.web.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class HealthController {

    @GetMapping
    fun get(): ResponseEntity<*> {
        return ResponseEntity.ok("")
    }
}