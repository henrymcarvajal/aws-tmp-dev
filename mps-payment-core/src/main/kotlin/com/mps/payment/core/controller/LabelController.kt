package com.mps.payment.core.controller

import org.springframework.http.ResponseEntity
import com.mps.payment.core.service.LabelService
import org.springframework.core.io.InputStreamResource
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayInputStream

import java.io.IOException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.time.LocalDateTime
import java.util.UUID


@RestController
@RequestMapping(path = ["label"])
class LabelController(
        private val labelService: LabelService
) {

    @GetMapping("/public/order/{id}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    @Throws(IOException::class)
    fun getPdf(@PathVariable id: UUID): ResponseEntity<*> {
        val labels = labelService.getPdf(id)
                ?: return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        "Error generando archivo"
                )
        return generateResponsePDF(labels)
    }

    @GetMapping("/public/multiple/order/{ids}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    @Throws(IOException::class)
    fun getPdfForMultipleOrders(@PathVariable ids: Array<UUID>): ResponseEntity<*> {
        val label = labelService.getPdf(ids)
                ?: return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        "Error generando archivo"
                )
        return generateResponsePDF(label)
    }

    private fun generateResponsePDF(bis: ByteArrayInputStream?): ResponseEntity<*> {
        return bis?.let {
            val headers = HttpHeaders()
            headers.add("Content-Disposition", "inline; filename=${LocalDateTime.now()}.pdf")
            ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body<InputStreamResource>(InputStreamResource(it))
        } ?: let {
            ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generando documento")
        }
    }
}