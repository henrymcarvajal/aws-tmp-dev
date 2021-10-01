package com.mps.payment.core.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class InternalNumbers(@Id val id: UUID,
                           @Column(name = "order_id") val orderId: UUID,
                           @Column(name = "payment_comission") var paymentCommission: BigDecimal,
                           @Column(name = "freight_comission") var freightCommission: BigDecimal?,
                           @Column(name = "payment_method") val paymentMethod: String,
                           @Column(name = "creation_date") val creationDate: LocalDateTime = LocalDateTime.now(),
                           @Column(name = "last_updated") var modificationDate: LocalDateTime
)