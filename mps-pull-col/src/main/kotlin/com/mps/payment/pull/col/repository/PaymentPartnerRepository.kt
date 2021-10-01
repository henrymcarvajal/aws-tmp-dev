package com.mps.payment.pull.col.repository

import com.mps.payment.pull.col.model.PaymentPartner
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


@Repository
interface PaymentPartnerRepository: CrudRepository<PaymentPartner, String> {
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE payment_partner  SET final_status= :result, modificationdate= :now WHERE id = :partnerId", nativeQuery = true)
    fun updateFinalResultOfPayment(
            @Param("partnerId") partnerId:String,
            @Param("result") result: String,
            @Param("now") modificationDate: LocalDateTime = LocalDateTime.now()
    )
}