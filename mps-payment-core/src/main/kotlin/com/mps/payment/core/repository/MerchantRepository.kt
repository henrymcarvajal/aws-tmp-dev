package com.mps.payment.core.repository

import com.mps.payment.core.model.Merchant
import com.mps.payment.core.model.Payment
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MerchantRepository: CrudRepository<Merchant, UUID> {

    @Query(value = "SELECT * FROM Merchant m WHERE m.nit = :nit", nativeQuery = true)
    fun findMerchantByNit(
            @Param("nit") nit: String): Merchant?

    fun findByEmail(
            @Param("email") email: String): List<Merchant>

    fun existsByEmail(@Param("email") email: String): Boolean

    @Query(value = "select * from Merchant M where CAST(M.id AS text)  LIKE %:shortId%",nativeQuery = true)
    fun getMerchantByShortId(
            @Param("shortId") shortId: String): Merchant?
}