package com.mps.payment.core.repository

import com.mps.payment.core.model.PaymentState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentStateRepository: JpaRepository<PaymentState,Int> {
}