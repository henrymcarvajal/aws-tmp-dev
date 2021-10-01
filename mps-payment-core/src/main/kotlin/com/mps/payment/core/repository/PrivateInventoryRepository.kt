package com.mps.payment.core.repository

import com.mps.payment.core.model.Inventory
import com.mps.payment.core.model.PrivateInventory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PrivateInventoryRepository : JpaRepository<PrivateInventory, UUID>, JpaSpecificationExecutor<PrivateInventory> {
    fun findByProductIdAndMerchantIdAndSellerMerchantIdAndDisabledFalse(
            productId: UUID,
            merchantId: UUID,
            sellerMerchantId: UUID
    ): Optional<PrivateInventory>

    fun findByProductIdAndSellerMerchantIdAndDisabledFalse(
            productId: UUID,
            sellerMerchantId: UUID
    ): Optional<PrivateInventory>

    fun findBySellerMerchantIdAndDisabledFalse(sellerMerchantId: UUID): List<PrivateInventory>

    fun findByMerchantIdAndDisabledFalse(providerId:UUID):List<PrivateInventory>

    fun findByProductId(providerId:UUID):List<PrivateInventory>
}