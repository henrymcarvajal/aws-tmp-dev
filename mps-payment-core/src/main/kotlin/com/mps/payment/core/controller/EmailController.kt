package com.mps.payment.core.controller

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.email.EmailSender
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@RestController
@RequestMapping(path = ["email"])
class EmailController(private val emailSender: EmailSender) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    fun sendEmail(@Valid @RequestBody sendEmailRequest: SendEmailRequest): ResponseEntity<*> {
        log.info("sending email $sendEmailRequest")
        return when (val responseService = emailSender.sendEmailWithTemplate(receiver = sendEmailRequest.receiver,
                o = sendEmailRequest.params, templateName = sendEmailRequest.templateName, title = sendEmailRequest.title)) {
            is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as String)
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

}

data class SendEmailRequest(
        @get:NotBlank(message = "receiver can not be empty or null")
        val receiver: String,
        val params: Map<String, String>,
        @get:NotBlank(message = "title can not be empty or null")
        val title: String,
        @get:NotBlank(message = "templateName can not be empty or null")
        val templateName: String
)