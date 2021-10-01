package com.mps.payment.core.repository

import com.mps.payment.core.model.BankingInformation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BankingInformationRepository:JpaRepository<BankingInformation, UUID> {

    fun findByMerchantId(merchantId: UUID): Optional<BankingInformation>
}