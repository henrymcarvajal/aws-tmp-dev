package com.mps.payment.core.controller

import com.mps.payment.core.client.sms.SendMessageRequestInput
import com.mps.payment.core.client.sms.SmsClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/sms")
class SmsController(
        private val smsClient: SmsClient
) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    fun sendSmsMessage(@RequestBody smsDetails: SmsDetails): ResponseEntity<Boolean> {
        val input = SendMessageRequestInput(smsDetails.number, smsDetails.message)
        val response = smsClient.sendSmsMessage(input)
        return ResponseEntity(response, HttpStatus.OK)
    }
}

data class SmsDetails(
    val number: String,
    val message: String
)
