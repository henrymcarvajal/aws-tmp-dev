package com.mps.payment.pull.col.service

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.WooPayment
import com.mps.payment.pull.col.client.PaymentCoreClient
import com.mps.payment.pull.col.model.RequestWoocommerce
import com.mps.payment.pull.col.model.toCustomerDTO
import com.mps.payment.pull.col.model.toPaymentDTO
import com.mps.payment.pull.col.util.generateSignature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class WooService(private val paymentCoreClient: PaymentCoreClient) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun processPaymentFromCheckout(request: RequestWoocommerce):GenericResponse<*> {

        if(!validateSignature(request)){
            log.error("woocommerce integration processPaymentFromCheckout error validating signature")
            return GenericResponse.ErrorResponse("woocommerce integration processPaymentFromCheckout " +
                    "error validating signature")
        }
        val payment = request.toPaymentDTO()
        val customerDTO = request.toCustomerDTO()
        val agreePaymentResponse = paymentCoreClient.processPayment(WooPayment(payment,customerDTO))
                ?: return GenericResponse.ErrorResponse("agree could not be executed")
        return if(agreePaymentResponse==null){
            GenericResponse.ErrorResponse("Payment could not be created")
        }else{
            GenericResponse.SuccessResponse(agreePaymentResponse)
        }
    }

    private fun validateSignature(request: RequestWoocommerce):Boolean{
        val secret= request.merchantId.take(6)
        val calculatedSignature = generateSignature(buildMessageToSignature(request,secret),secret)
        return request.signature == calculatedSignature
    }

    private fun buildMessageToSignature(request: RequestWoocommerce,key:String):String = "${request.currency}+${request.description}" +
            "+${request.email}+${request.firstName}+${request.lastName}+${request.merchantId}+${request.numberContact}+${request.orderId}+" +
            "${request.testMode}+${request.total}+$key"
}