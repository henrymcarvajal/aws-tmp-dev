package com.mps.common.dto

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.constraints.Min

data class PaymentDTO(var id: UUID?,
                      @get:NotNull(message = "id Merchant can not be null") var idMerchant: UUID?,
                      var idCustomer: UUID?,
                      @get:NotNull(message = "amount can not be null") @get:Min(value = 3000,
                              message = "El valor de la transacción no puede ser menor a $20.000") var amount: BigDecimal?,
                      val creationDate: LocalDateTime?=null,
                      val modificationDate: LocalDateTime?=null,
                      var idState: Int?,
                      var linkUrl: String?,
                      val guideNumber: String?,
                      val transportCompany: String?,
                      var description: String?,
                      var closeDate: LocalDateTime?=null,
                      var withdrawal: UUID?= null,
                      var isAboutToClose:Boolean?=false,
                      var comision: BigDecimal?=null,
                      val publicId: String? = null,
                      var productId:UUID?=null
){
    constructor() : this(null,null,null, null,
            null, null,null,null,
            "","","", null) {

    }
}

data class PaymentStateInput(
        @get:NotNull(message = "payment id is mandatory") var paymentId:String,
        @get:Min(1,message = "invalid state") val state:Int,
        val guideNumber: String?=null,
        val transportCompany: String?=null
)

data class PaymentAgree(
        @get:NotNull(message = "payment/product id is mandatory")
        var idPayment: String?,
        @get:Valid
        @get:NotNull(message = "customer is mandatory")
        val customer: CustomerDTO
)

data class WooPayment(
        @get:NotNull(message = "payment is mandatory")
        val payment:PaymentDTO,
        @get:NotNull(message = "customer is mandatory")
        val customer:CustomerDTO
)