package com.mps.common.dto

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.validation.constraints.*

data class OrderDropDTO(
        var id: UUID?,
        val creationDate: LocalDateTime?= LocalDateTime.now(),
        val modificationDate: LocalDateTime?= LocalDateTime.now(),
        var orderStatus: Int?,
        val guideNumber: Int?,
        var comision: BigDecimal?=null,
        @Pattern(regexp = "MPS|COD", flags = [Pattern.Flag.CASE_INSENSITIVE]) val paymentMethod:String,
        var productId: UUID,
        var customer: CustomerDTO?,
        val quantity:Int,
        var paymentId:String?,
        var amount: BigDecimal,
        val freightPrice: BigDecimal?,
        val observations:String,
        var branchCode:Int=0,
        var isLabeled:Boolean,
        var sellerName:String?,
        val label:String?
)

data class UpdateOrderRequest(
        @get:NotNull(message = "id es obligatorio")
        var id: UUID,
        @get:NotNull(message = "información de cliente es obligatorio")
        var customer: CustomerDTO
)