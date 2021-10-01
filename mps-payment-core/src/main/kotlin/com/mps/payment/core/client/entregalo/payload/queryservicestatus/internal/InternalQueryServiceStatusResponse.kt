package com.mps.payment.core.client.entregalo.payload.queryservicestatus.internal

import org.springframework.http.HttpStatus

data class InternalQueryServiceStatusResponse(
    val status: HttpStatus,
    val serviceStatus: String? = null
)