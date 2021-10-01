package com.mps.payment.core.model

import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Inventory(
    @Id var id: UUID,
    var merchantId: UUID,
    var branchId: UUID,
    var productId: UUID,
    var quantity: Int,
    @Column(name = "created_at") var creationDate: LocalDateTime = LocalDateTime.now(),
    @Column(name = "last_updated") var modificationDate: LocalDateTime? = LocalDateTime.now(),
    var disabled: Boolean = false,
    @Column(name = "disabled_at") var deletionDate: LocalDateTime? = LocalDateTime.now()
) {
    companion object {
        const val PRODUCT_ID = "productId"
        const val DISABLED = "disabled"
    }
}

data class InventoryDTO(
    var id: UUID? = UUID.randomUUID(),
    var merchantId: UUID,
    var branchId: UUID,
    var productId: UUID,
    var quantity: Int,
    var creationDate: LocalDateTime = LocalDateTime.now(),
    var modificationDate: LocalDateTime? = null,
    var disabled: Boolean = false,
    var deletionDate: LocalDateTime? = null
)

fun Inventory.toDTO() = InventoryDTO(
    id = this.id,
    merchantId = this.merchantId,
    branchId = this.branchId,
    productId = this.productId,
    quantity = this.quantity,
    creationDate = this.creationDate,
    modificationDate = this.modificationDate,
    disabled = this.disabled,
    deletionDate = this.deletionDate
)

fun InventoryDTO.toEntity() = Inventory(
    id = this.id ?: UUID.randomUUID(),
    merchantId = this.merchantId,
    branchId = this.branchId,
    productId = this.productId,
    quantity = this.quantity,
    creationDate = this.creationDate,
    modificationDate = this.modificationDate,
    disabled = this.disabled,
    deletionDate = this.deletionDate
)