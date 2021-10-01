package com.mps.payment.core.client.entregalo.payload

import org.springframework.http.HttpStatus
import java.math.BigDecimal

data class QueryStatusRequest(
        val idShipping: Int
)

data class QueryServiceStatusResponse(
        val status: HttpStatus,
        val serviceStatus: String? = null,
        val freightPrice: BigDecimal?=null
)