package com.mps.payment.core.repository

import com.mps.payment.core.model.Inventory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface InventoryRepository : JpaRepository<Inventory, UUID>, JpaSpecificationExecutor<Inventory> {
    fun findByBranchIdAndProductIdAndDisabledNotNullAndDisabledFalse(branchId: UUID, productId: UUID): List<Inventory>

    fun findByProductIdAndDisabledNotNullAndDisabledFalse(productId: UUID): List<Inventory>

    @Query("select CAST(i.id as TEXT) as inventoryId, i.quantity, b.branch_code as branchCode, dc.latitude , dc.longitude" +
            "  from inventory i " +
            "  join branch b on b.id = i.branch_id " +
            "  join dane_code dc on dc.id = b.dane_code_id" +
            " where CAST(i.product_id as TEXT) like %:productId%" +
            "   and i.disabled = false" +
            "   and b.disabled = false", nativeQuery = true)
    fun findByProductIdGeolocalized (productId: UUID): List<GeolocalizedInventory>
    //
}

interface GeolocalizedInventory {
    val inventoryId: String?
    val quantity: Int?
    val branchCode: Int?
    val latitude: Double?
    val longitude: Double?
}
