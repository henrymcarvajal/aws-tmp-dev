package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.PaymentStateInput
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CashInService(private val paymentService: PaymentService, private val orderService: OrderService,
                    private val logisticPartnerService: LogisticPartnerService) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun updatePaidStateCashIn(paymentStateInput: PaymentStateInput): GenericResponse<*> {
        val orderResponse = orderService.findByPaymentId(paymentStateInput.paymentId)
        if (orderResponse.isPresent) {
            log.info("order response", orderResponse)
            logisticPartnerService.requestFreightMPS(paymentStateInput.paymentId,updateFunction)
        }
        return paymentService.updateStateOfPayment(paymentStateInput)
    }

    val updateFunction = {paymentId: String , guide: Int,label:String -> orderService.updateGuideToOrder(paymentId,guide,label)}
}