package com.mps.payment.core.model

import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.validation.constraints.Min

@Entity
data class PrivateInventory(
    @Id var id: UUID,
    var merchantId: UUID?,
    var sellerMerchantId: UUID,
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

data class PrivateInventoryDTO(
    var id: UUID? = UUID.randomUUID(),
    var merchantId: UUID?,
    var sellerMerchantId: UUID,
    var productId: UUID,
    @get:Min(value = 1, message = "Quantity must be minimum 1")
    var quantity: Int,
    var creationDate: LocalDateTime = LocalDateTime.now(),
    var modificationDate: LocalDateTime? = null,
    var disabled: Boolean = false,
    var deletionDate: LocalDateTime? = null
)

data class PrivateInventoryView(
        var id: UUID,
        var dropSellerName: String,
        var productName: String,
        var quantity: Int,
        var creationDate: LocalDateTime
)

fun PrivateInventory.toDTO() = PrivateInventoryDTO(
    id = this.id,
    merchantId = this.merchantId,
    sellerMerchantId = this.sellerMerchantId,
    productId = this.productId,
    quantity = this.quantity,
    creationDate = this.creationDate,
    modificationDate = this.modificationDate,
    disabled = this.disabled,
    deletionDate = this.deletionDate
)

fun PrivateInventoryDTO.toEntity() = PrivateInventory(
    id = this.id ?: UUID.randomUUID(),
    merchantId = this.merchantId,
    sellerMerchantId = this.sellerMerchantId,
    productId = this.productId,
    quantity = this.quantity,
    creationDate = this.creationDate,
    modificationDate = this.modificationDate,
    disabled = this.disabled,
    deletionDate = this.deletionDate
)