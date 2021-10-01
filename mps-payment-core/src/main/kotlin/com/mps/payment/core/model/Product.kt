package com.mps.payment.core.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull     

@Entity
data class Product(
        @Id var id: UUID,
        var description: String,
        var amount: BigDecimal,
        @Column(name = "merchant_id") var merchantId: UUID,
        @Column(name = "precio_dropshipping") var dropshippingPrice: BigDecimal?,
        @Column(name = "created_at") var creationDate: LocalDateTime? = LocalDateTime.now(),
        @Column(name = "inventory") var inventory: Int,
        @Column(name = "name") var name: String?,
        @Column(name = "img_url") var imageUrl: String? = null,
        @Column(name = "img_url_min") var imageUrlMin: String? = null,
        @Column(name = "dropshipping") var dropshipping: Boolean?,
        @Column(name = "special_features") var specialFeatures: Boolean=false,
        @Column(name = "disabled") var disabled: Boolean,
        @Column(name = "disabled_at") var deletionDate: LocalDateTime?,
        @Column(name = "category") var category: Int
)

data class ProductDTO(
        var id: UUID?,
        @get:NotNull(message = "description can not be null")
        @get:NotEmpty(message = "description can not be empty")
        var description: String,
        @get:NotNull(message = "amount can not be null")
        @get:Min(value = 3000, message = "El valor de la transacción no puede ser menor a $20.000")
        var amount: BigDecimal,
        @get:NotNull(message = "merchantId can not be null")
        var merchantId: UUID,
        var creationDate: LocalDateTime? = LocalDateTime.now(),
        var shortId: String? = "",
        var name: String? = null,
        var imageUrl: String?,
        var imageUrlMin: String?,
        @get:Min(value = 1, message = "La cantidad de inventario no puede ser menor a 1")
        var inventory: Int,
        @get:Min(value = 1, message = "la categoría no puede ser menor a 1")
        @get:Max(value = 9, message = "la categr[ia no puede ser mayor a 9")
        var category: Int,
        var dropshipping: Boolean? = false,
        var specialFeatures: Boolean = false,
        var disabled: Boolean? = false,
        var deletionDate: LocalDateTime? = null,
        var dropshippingPrice: BigDecimal?
)

data class UpdateProductRequest(
        @get:NotNull(message = "id can not be null")
        @get:NotEmpty(message = "id can not be empty")
        val shortId: String,
        val amount: BigDecimal,
        val description: String,
        val inventory: Int,
        val name: String,
        val dropshipping: Boolean,
        val dropshippingPrice: BigDecimal?
)

data class UpdateInventoryProductRequest(
        @get:NotNull(message = "id no puede ser nulo")
        val id: UUID,
        @get:NotNull(message = "Inventario no puede ser nulo")
        @get:Min(1)
        val inventory: Int
)

fun Product.toDTO() = ProductDTO(id = this.id, description = this.description, amount = this.amount,
        merchantId = this.merchantId, shortId = this.id.toString().takeLast(6), inventory = this.inventory,
        imageUrl = this.imageUrl, dropshipping = this.dropshipping, disabled = this.disabled,
        deletionDate = this.deletionDate, name = this.name, dropshippingPrice = this.dropshippingPrice,
        imageUrlMin = this.imageUrlMin,specialFeatures = this.specialFeatures, category = this.category
)

fun ProductDTO.toEntity() = Product(id = this.id
        ?: UUID.randomUUID(), description = this.description, amount = this.amount,
        merchantId = this.merchantId, inventory = this.inventory, imageUrl = this.imageUrl,
        dropshipping = this.dropshipping, disabled = this.disabled ?: false, deletionDate = this.deletionDate,
        name = this.name, dropshippingPrice = this.dropshippingPrice,
        imageUrlMin = this.imageUrlMin,specialFeatures = this.specialFeatures, category = this.category
)