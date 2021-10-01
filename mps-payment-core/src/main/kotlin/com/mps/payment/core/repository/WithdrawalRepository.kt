package com.mps.payment.core.repository

import com.mps.payment.core.model.Withdrawal
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WithdrawalRepository: CrudRepository<Withdrawal,UUID> {

    @Query(value = "SELECT * FROM Withdrawal w WHERE w.id_merchant = :merchantId ORDER BY w.application_date DESC", nativeQuery = true)
    fun findWithdrawalByIdMerchantByDesc(
            @Param("merchantId") merchantId: UUID): List<Withdrawal>
}