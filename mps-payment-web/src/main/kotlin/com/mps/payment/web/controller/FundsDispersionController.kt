package com.mps.payment.web.controller

import com.mps.common.dto.ServiceResponse
import com.mps.payment.core.service.fundsdispersion.FundsDispersionService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping(path = ["dispersion"])
class FundsDispersionController(
    private val fundsDispersionService: FundsDispersionService
) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])//, produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun generateDispersion(@RequestPart("excel") excel: MultipartFile?): ResponseEntity<*> {
        return try {
            when (val responseService = fundsDispersionService.generateDispersion(excel)) {
                is ServiceResponse.Success -> {
                    if (excel != null) {
                        cleanUploadedFile(excel)
                    }
                    generateDispersionExcel(responseService.obj as ByteArrayInputStream)
                }
                is ServiceResponse.Error -> {
                    if (excel != null) {
                        cleanUploadedFile(excel)
                    }
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(mapOf("errorMessage" to responseService.message))
                }
            }
        } catch (e: Exception) {
            log.error("Error generating dispersion", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error generating dispersion")
        }
    }

    private fun generateDispersionExcel(bis: ByteArrayInputStream?): ResponseEntity<*> {
        return bis?.let {
            val headers = HttpHeaders()
            headers.add("Content-Disposition", "inline; filename=${getFilename()}")
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

    private fun getFilename() : String {
        var formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        return "dispersion-${LocalDateTime.now().format(formatter)}.xlsx"
    }

    private fun cleanUploadedFile(multipartFile: MultipartFile) {
        try {
            multipartFile.inputStream.close()
            Files.delete(Path.of(multipartFile.originalFilename))
        } catch (e: Exception) {
            log.error("Error cleaning up temporary uploaded file", e)
        }
    }
}