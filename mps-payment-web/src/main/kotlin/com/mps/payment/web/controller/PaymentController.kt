package com.mps.payment.web.controller

import com.mps.common.dto.*
import com.mps.payment.core.model.*
import com.mps.payment.core.security.policy.ContextAwarePolicyEnforcement
import com.mps.payment.core.service.CashInService
import com.mps.payment.core.service.CustomerService
import com.mps.payment.core.service.MerchantService
import com.mps.payment.core.service.PaymentService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.validation.Valid

@RestController
@RequestMapping(path = ["payment"])
class PaymentController(
        private val paymentService: PaymentService,
        private val customerService: CustomerService,
        private val merchantService: MerchantService,
        private val cashInService: CashInService,
        private val policyEnforcement: ContextAwarePolicyEnforcement
) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    fun createPayment(@Valid @RequestBody paymentDTO: PaymentDTO): ResponseEntity<*> {
        log.info("input $paymentDTO")
        val merchant = merchantService.getMerchant(paymentDTO.idMerchant!!)
        return if (merchant.isEmpty) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to "Merchant does not exist"))
        } else {
            val merchantVal = merchant.get()
            return if (merchantVal.isEnabled != null && merchantVal.isEnabled as Boolean) {
                when (val responseService = paymentService.createPayment(paymentDTO)) {
                    is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as PaymentDTO)
                    is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(mapOf("errorMessage" to responseService.message))
                }
            } else {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(mapOf("errorMessage" to "Merchant is not authorized yet"))
            }
        }
    }

    @PostMapping("/agree")
    fun agreePayment(@Valid @RequestBody paymentAgree: PaymentAgree): ResponseEntity<*> {
        log.info("input $paymentAgree")
        return when (val responseService = paymentService.agreePayment(paymentAgree)) {
            is GenericResponse.SuccessResponse -> {
                ResponseEntity.ok().body(responseService.obj as String)
            }
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    @PostMapping("/woo")
    fun processWooPayment(@Valid @RequestBody wooPayment: WooPayment): ResponseEntity<*> {
        log.info("input $wooPayment")
        return when (val responseService = paymentService.processWooPayment(wooPayment)) {
            is GenericResponse.SuccessResponse -> {
                ResponseEntity.ok().body(responseService.obj as String)
            }
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    @GetMapping("/{id}")
    fun getPayment(@PathVariable id: String, isInternal: Boolean = false, shortId: String? = null): ResponseEntity<PaymentDTO> {
        var payment: Payment? = null
        val valuePayment = if ((shortId != null && isInternal) || id.length==6) {
            val paymentList = paymentService.getPaymentByShortId(shortId?:id)
            if (paymentList.isEmpty()) {
                return ResponseEntity.notFound().build()
            } else {
                payment = paymentList[0]
                payment.toDTO()
            }
        } else {
            val optionPayment = paymentService.getPayment(UUID.fromString(id))
            if (optionPayment.isEmpty) {
                return ResponseEntity.notFound().build()
            } else {
                payment = optionPayment.get()
                payment.toDTO()
            }
        }
        if (isInternal) {
            valuePayment.id = null
            valuePayment.idCustomer = null
            valuePayment.withdrawal = null
        }
        if (valuePayment.closeDate != null) {
            valuePayment.isAboutToClose = LocalDate.now().plusDays(1).isEqual((valuePayment.closeDate as LocalDateTime).toLocalDate())
        }
        if (!isInternal) {
            policyEnforcement.checkPermission(payment, "PAYMENT_READ")
        }
        return ResponseEntity.ok().body(valuePayment)
    }

    @GetMapping("/public/{id}")
    fun getPaymentPublic(@PathVariable id: String): ResponseEntity<PaymentDTO> {
        return getPayment("", true, id)
    }

    @GetMapping("/customer/{id}")
    fun getCustomerOfPayment(@PathVariable id: UUID): ResponseEntity<CustomerDTO> {
        val customer = customerService.getCustomerById(id)
        return if (customer.isEmpty) {
            ResponseEntity.notFound().build()
        } else {
            //policyEnforcement.checkPermission(payment.get(), "PAYMENT_READ")
            ResponseEntity.ok().body(customer.get().toDTO())
        }
    }

    @GetMapping("/merchant")
    fun getPaymentPerMerchant(@RequestParam("merchantId") merchantId: UUID,
                              @RequestParam("durationInDays") duration: Int?,
                              @RequestParam("paymentState") paymentState: Int?
    ): ResponseEntity<List<PaymentWithCustomerDTO>> {
        var queryParams: QueryParams? = null
        if (duration != null || paymentState != null) {
            queryParams = QueryParams(duration = duration ?: 0, state = paymentState ?: 0)
        }
        val payments = paymentService.getPaymentsPerMerchant(merchantId, queryParams)
        return if (payments.isEmpty()) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok().body(payments)
        }
    }

    @GetMapping("/merchant/closed/{id}")
    fun getAmountOfClosedPaymentPerMerchant(@PathVariable id: UUID): ResponseEntity<BigDecimal> {
        return ResponseEntity.ok(merchantService.getAmountOfClosedPayments(id))
    }

    @PatchMapping("/updateState")
    fun updatePaymentState(@Valid @RequestBody paymentStateInput: PaymentStateInput): ResponseEntity<*> {
        return when (val responseService = paymentService.updateStateOfPayment(PaymentStateInput(paymentId = paymentStateInput.paymentId!!,
                state = paymentStateInput.state, guideNumber = paymentStateInput.guideNumber, transportCompany = paymentStateInput.transportCompany))) {
            is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as PaymentDTO)
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    @PatchMapping("/updateState/notification")
    fun updatePaymentCashIn(@Valid @RequestBody paymentStateInput: PaymentStateInput): ResponseEntity<*> {
        return when (val responseService = cashInService.updatePaidStateCashIn(PaymentStateInput(paymentId = paymentStateInput.paymentId!!,
                state = paymentStateInput.state, guideNumber = paymentStateInput.guideNumber, transportCompany = paymentStateInput.transportCompany))) {
            is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as PaymentDTO)
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    @PatchMapping("/delayClose")
    fun delayClose(@Valid @RequestBody delayCloseDateInput: DelayCloseDateToPaymentInput): ResponseEntity<*> {
        return when (val responseService = paymentService.delayCloseDate(delayCloseDateInput)) {
            is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as PaymentDTO)
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }

    @PatchMapping("/public/updateState")
    fun updatePaymentStatePublic(@Valid @RequestBody paymentStateInput: PaymentStatePublicInput): ResponseEntity<*> {
        return when (val responseService = paymentService.updateStatePublicOfPayment(shortId = paymentStateInput.paymentId!!,
                state = paymentStateInput.state, numberId = paymentStateInput.numberId!!)) {
            is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as PaymentDTO)
            is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))
        }
    }
}