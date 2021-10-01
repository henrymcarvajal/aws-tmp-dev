package com.mps.payment.web.controller

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.MerchantDTO
import com.mps.common.dto.PutBalanceRequest
import com.mps.payment.core.model.*
import com.mps.payment.core.security.policy.ContextAwarePolicyEnforcement
import com.mps.payment.core.service.MerchantService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid

@RestController
@RequestMapping(path = ["merchant"])
class MerchantController(
        private val merchantService: MerchantService,
        private val policyEnforcement: ContextAwarePolicyEnforcement
) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    //@PreAuthorize("hasRole('ADMIN')")
    fun createMerchant(@Valid @RequestBody merchantDTO: MerchantDTO): ResponseEntity<*> {
        log.info("input $merchantDTO")
        return when (val responseService = merchantService.createMerchant(merchantDTO)) {
            is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as MerchantDTO)
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    @PutMapping
    //@PreAuthorize("hasRole('ADMIN')")
    fun updateMerchant(@RequestBody merchantDTO: MerchantDTO): ResponseEntity<*> {
        log.info("input $merchantDTO")
        return when (val responseService = merchantService.updateMerchant(merchantDTO)) {
            is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as MerchantDTO)
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    @PatchMapping
    fun updatePixel(@RequestBody pixelInfo: Map<String,String>): ResponseEntity<*> {
        log.info("update pixel input $pixelInfo")
        return when (val responseService = merchantService.updatePixel(pixelInfo)) {
            is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as MerchantDTO)
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    @PatchMapping("/balance")
    fun putBalance(@RequestBody request: PutBalanceRequest): ResponseEntity<*> {
        log.info("putBalance input $request")
        return when (val responseService = merchantService.putBalance(request)) {
            is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as MerchantDTO)
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    @PostMapping("/landing")
    //@PreAuthorize("hasRole('ADMIN')")
    fun createMerchantLanding(@Valid @RequestBody merchantLandingtDTO: MerchantLandingtDTO): ResponseEntity<*> {
        log.info("input $merchantLandingtDTO")
        return when (val responseService = merchantService.createMerchant(merchantLandingtDTO.toMerchantDTO())) {
            is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as MerchantDTO)
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    @GetMapping("/{id}")
    fun getMerchant(@PathVariable id: UUID): ResponseEntity<MerchantDTO> {

        log.info("getting merchant $id")
        val merchant = merchantService.getMerchant(id)
        return if (merchant.isEmpty) {
            ResponseEntity.notFound().build()
        } else {
            policyEnforcement.checkPermission(merchant.get(), "MERCHANT_READ")
            ResponseEntity.ok().body(merchant.get().toDto())
        }
    }

    @GetMapping("/visible/{id}")
    fun getOverviewMerchant(@PathVariable id: UUID): ResponseEntity<MerchantLandingtDTO> {

        log.info("getting merchant $id")
        val merchant = merchantService.getMerchant(id)
        return if (merchant.isEmpty) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok().body(merchant.get().toPublicDTO())
        }
    }

    @GetMapping("/public/{id}")
    fun getMerchantName(@PathVariable id: UUID): ResponseEntity<Map<String,String?>> {

        log.info("getting merchant name $id")
        val merchant = merchantService.getMerchant(id)
        return if (merchant.isEmpty) {
            ResponseEntity.notFound().build()
        } else {
            val merchantName = merchant.get().toDto().name
            ResponseEntity.ok().body(mapOf("name" to merchantName,"fbId" to merchant.get().fbPixel))
        }
    }
}