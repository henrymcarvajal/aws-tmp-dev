package com.mps.payment.core.repository

import com.mps.payment.core.model.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface PaymentRepository : JpaRepository<Payment, UUID>,PaymentRepositoryCustom {

    @Query(value = "SELECT * FROM Payment p WHERE p.id_merchant = :merchantId ORDER BY p.created_at DESC", nativeQuery = true)
    fun findPaymentByIdMerchantByDesc(
            @Param("merchantId") merchantId: UUID): List<Payment>

    @Query(value = "SELECT * FROM Payment p WHERE p.withdrawal = :withdrawalId ORDER BY p.created_at DESC", nativeQuery = true)
    fun findPaymentsByWithdrawal(
            @Param("withdrawalId") withdrawalId: UUID): List<Payment>

    @Query(value = "SELECT * FROM Payment p WHERE p.id_merchant = :merchantId AND p.id_payment_state= :state", nativeQuery = true)
    fun findPaymentByIdMerchantAndState(
            @Param("merchantId") merchantId: UUID,@Param("state") state: Int): List<Payment>

    @Query(value = "SELECT * FROM Payment p WHERE p.id_payment_state = :state", nativeQuery = true)
    fun getAllPaymentsByState(
            @Param("state") state: Int): List<Payment>

    @Query(value = "SELECT * FROM Payment p WHERE CAST(p.last_updated AS DATE) = CAST(:maxDate AS DATE) and p.id_payment_state IN :state", nativeQuery = true)
    fun getPaymentsByDateAndState(
            @Param("maxDate") maxDate: LocalDate, @Param("state")state: List<Int>): List<Payment>

    @Query(value = "SELECT * FROM Payment p WHERE CAST(p.close_date AS DATE) = CAST(:maxDate AS DATE) and p.id_payment_state IN :state", nativeQuery = true)
    fun getPaymentsByCloseDateAndState(
            @Param("maxDate") maxDate: LocalDate, @Param("state")state: List<Int>): List<Payment>

    @Query(value = "select * from Payment p where CAST(p.id AS text)  LIKE %:shortId%",nativeQuery = true)
    fun getPaymentByShortId(
            @Param("shortId") shortId: String): List<Payment>

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE Payment  SET id_payment_state= :newState, last_updated= :now WHERE id = :paymentId", nativeQuery = true)
    fun updateStateOfPayment(
            @Param("paymentId") paymentId: UUID, @Param("newState") state: Int,
            @Param("now") updatedDate:LocalDateTime = LocalDateTime.now()
    )

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE Payment  SET id_payment_state= :newState, last_updated= :now, guide_number = :guideNumber, transport_company= :transportCompany WHERE id = :paymentId", nativeQuery = true)
    fun updateStateAndDeliveryInfoOfPayment(
            @Param("paymentId") paymentId: UUID, @Param("newState") state: Int,
            @Param("guideNumber") guideNumber:String, @Param("transportCompany") transportCompany:String,
            @Param("now") updatedDate:LocalDateTime = LocalDateTime.now()
    )
}