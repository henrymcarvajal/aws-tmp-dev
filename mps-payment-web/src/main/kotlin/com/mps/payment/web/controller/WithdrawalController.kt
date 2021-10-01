package com.mps.payment.web.controller

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.PaymentDTO
import com.mps.payment.core.model.WithdrawalDTO
import com.mps.payment.core.model.toDTO
import com.mps.payment.core.service.WithdrawalService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID
import javax.validation.Valid


@RestController
@RequestMapping(path = ["withdrawal"])
class WithdrawalController(private val withdrawalService: WithdrawalService) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    fun createWithdrawal(@Valid @RequestBody withdrawalDTO: WithdrawalDTO): ResponseEntity<*> {
        log.info("input $withdrawalDTO")
        return when (val responseService = withdrawalService.createWithdrawal(withdrawalDTO)) {
            is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as WithdrawalDTO)
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    @GetMapping("/merchant/{id}")
    fun getWithdrawalByMerchant(@PathVariable id: UUID): ResponseEntity<List<WithdrawalDTO>> {
        val withdrawals = withdrawalService.getWithdrawalByService(id)
        return if (withdrawals.isEmpty()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok().body(withdrawals.map { it.toDTO() })
        }
    }

    @GetMapping("/payment/{id}")
    fun getPaymentsPerWithdrawal(@PathVariable id: UUID): ResponseEntity<List<PaymentDTO>> {
        val withdrawals = withdrawalService.getPaymentsByWithdrawal(id)
        return if (withdrawals.isEmpty()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok().body(withdrawals)
        }
    }
}