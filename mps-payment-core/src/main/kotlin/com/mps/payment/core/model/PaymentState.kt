package com.mps.payment.core.model

import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class PaymentState(
        @Id val id: Int, val name:String
)