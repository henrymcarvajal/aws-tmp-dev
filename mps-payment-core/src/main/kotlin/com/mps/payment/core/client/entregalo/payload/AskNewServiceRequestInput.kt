package com.mps.payment.core.client.entregalo.payload

import java.math.BigDecimal

data class AskNewServiceRequestInput(
    val branch: Int,
    val peopleState: String,
    val peopleCity: String, // dane code
    val peopleReceiver: String,
    val peoplePhone: Long,
    val declaredValue: BigDecimal,
    val amountToReceive: BigDecimal,
    val deliverySector:String,
    val address: String,
    val order:String,
    val observation:String?
)