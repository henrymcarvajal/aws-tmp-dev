package com.mps.payment.core.client.entregalo.payload.queryservicestatus.external

import java.math.BigDecimal

data class ExternalQueryServiceStatusData(
    val Guia: String,
    val Tracking: String,
    val Status: String,
    val Valor:BigDecimal
)