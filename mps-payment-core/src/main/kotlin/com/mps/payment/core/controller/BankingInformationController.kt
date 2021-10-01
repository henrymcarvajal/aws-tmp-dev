package com.mps.payment.core.controller

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.model.*
import com.mps.payment.core.service.BankingInformationService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid


@RestController
@RequestMapping(path = ["banking-info"])
class BankingInformationController(private val bankingInformationService: BankingInformationService) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)


    @PostMapping
    fun createOrUpdateBankingInfo(
            @Valid @RequestBody createBankingInformationRequest: CreateBankingInformationRequest
    ): ResponseEntity<*> {
        log.info("input createOrUpdateBankingInfo $createBankingInformationRequest")
        return when (val responseService = bankingInformationService.createOrUpdateBankingInformation(createBankingInformationRequest)) {
            is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as BankingInformationDTO)
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    @GetMapping("/{merchantId}")
    fun getBankInformationByMerchantId(
            request: HttpServletRequest, @PathVariable merchantId: UUID
    ): ResponseEntity<*> {
        log.info("getting bankinfo $merchantId")

        val token = request.getHeader("Authorization").replace("Bearer ", "")
        return when (val responseService = bankingInformationService.getBankInformationByMerchantId(merchantId, token)) {
            is GenericResponse.SuccessResponse -> {
                val response = responseService.obj as Optional<BankingInformation>
                if (response.isEmpty) {
                    ResponseEntity.notFound().build()
                } else {
                    ResponseEntity.ok().body(response.get().toDTO())
                }
            }
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }
}