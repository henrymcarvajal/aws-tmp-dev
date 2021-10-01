package com.mps.payment.core.client.entregalo.payload

import org.springframework.http.HttpStatus

data class AskNewServiceResponse(
    val status: HttpStatus,
    val guia: String? = null,
    val label:String=""
)