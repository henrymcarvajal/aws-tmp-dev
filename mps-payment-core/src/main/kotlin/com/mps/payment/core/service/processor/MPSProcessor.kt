package com.mps.payment.core.service.processor

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.OrderDropDTO
import com.mps.common.dto.PaymentAgree
import com.mps.payment.core.enum.OrderStatus
import com.mps.payment.core.model.*
import com.mps.payment.core.service.ProductService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class MPSProcessor(private val productService: ProductService): OrderProcessorTemplate() {
    override fun process(orderDropDTO: OrderDropDTO?, product: Product?, generalOrderDropEntity: GeneralOrderDrop?,
                         customer: Customer?, sellerMerchant: Merchant?) {

        val log: Logger = LoggerFactory.getLogger(this::class.java)
        generalOrderDropEntity!!.comision = BigDecimal.ZERO
        val paymentAgree = PaymentAgree(
                idPayment = product!!.id.toString().takeLast(6),
                customer = customer!!.toDTO()
        )
        when (val paymentResponse = productService
                .createPaymentFromProduct(paymentAgree, orderDropDTO!!.amount,sellerMerchant!!.id)) {
            is GenericResponse.SuccessResponse -> {
                generalOrderDropEntity.paymentId = paymentResponse.obj as String
                generalOrderDropEntity.orderStatus = OrderStatus.PAYMENT_PENDING.state
            }
            is GenericResponse.ErrorResponse -> {
                log.error("error creating payment from product")
                generalOrderDropEntity.orderStatus = OrderStatus.FAILED.state
            }
        }
    }
}