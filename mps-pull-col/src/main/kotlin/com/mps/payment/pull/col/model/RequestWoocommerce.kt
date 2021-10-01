package com.mps.payment.pull.col.model

import com.mps.common.dto.CustomerDTO
import com.mps.common.dto.PaymentAgree
import com.mps.common.dto.PaymentDTO
import com.mps.common.dto.PaymentStateInput
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class RequestWoocommerce(
        val total:String,
        val orderId: String,
        val firstName:String,
        val lastName:String,
        val email:String,
        val numberContact:String,
        val testMode:String,
        val currency: String,
        val description:String,
        val merchantId: String,
        val signature:String
)

fun RequestWoocommerce.toPaymentDTO() = PaymentDTO(
        UUID.randomUUID(), UUID.fromString(this.merchantId),null, this.total.toBigDecimal(), LocalDateTime.now(),null,1,
        null,null,null,this.description, LocalDateTime.now().plusDays(8)
)

fun RequestWoocommerce.toCustomerDTO()= CustomerDTO(
        email = this.email, contactNumber = this.numberContact.toString(),name = this.firstName, lastName = this.lastName
        ,id = UUID.randomUUID(),address = null,numberId = null,neighborhood = null,city = null,department = null
)