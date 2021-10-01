package com.mps.payment.core.model

import com.mps.payment.core.service.DiscountView
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

@Entity
data class DropshippingSale(
        @Id var id: UUID,
        @OneToOne(cascade = [CascadeType.ALL]) @JoinColumn(name = "product_id", referencedColumnName = "id") var product: Product,
        @OneToOne(cascade = [CascadeType.ALL]) @JoinColumn(name = "seller_merchant_id", referencedColumnName = "id") var merchant: Merchant,
        var amount: BigDecimal?,
        @Column(name = "special_conditions") var specialConditions: String?,
        @Column(name = "created_at") var creationDate: LocalDateTime? = LocalDateTime.now(),
        @Column(name = "disabled") var disabled: Boolean,
        @Column(name = "disabled_at") var deletionDate: LocalDateTime?
)

data class DropshippingSaleDTO(
        var id: UUID?,
        @get:NotNull(message = "productId can not be null")
        var productId: UUID,
        var sellerMerchantId: UUID?,
        val amount: BigDecimal?,
        var specialConditions: String?,
        var creationDate: LocalDateTime? = LocalDateTime.now(),
        var disabled: Boolean?,
        var deletionDate: LocalDateTime? = null,
)

data class DropshippingSaleCheckoutView(
        val productName: String,
        val productDescription: String,
        val sellerMerchantName: String,
        val imageURL: String?,
        val amount: BigDecimal,
        val specialConditions: Boolean,
        val discounts: List<DiscountView>,
        val fbId:String
)

data class ProductDropSale(
        val id: UUID,
        val buyPrice: BigDecimal,
        val sellPrice:BigDecimal,
        val name:String,
        val inventory:Int,
        val productId: UUID
)

data class UpdateAmountDropSale(
        @get:NotNull(message = "id no puede ser null")
        val id: UUID,
        @get:Min(3000)
        val amount: BigDecimal
)

fun  DropshippingSale.toProductSale(productDTO:ProductDTO) = ProductDropSale(
        id= this.id, name = productDTO.name?:"Nombre vacio",
        buyPrice = productDTO.dropshippingPrice?: BigDecimal.ZERO, sellPrice =this.amount?:productDTO.amount,
        inventory = productDTO.inventory, productId = this.product.id
)

fun DropshippingSale.toDTO()= DropshippingSaleDTO(id=this.id, productId = this.product.id,
        sellerMerchantId = this.merchant.id, amount=this.amount, specialConditions = this.specialConditions,
        creationDate = this.creationDate, disabled = this.disabled, deletionDate = this.deletionDate
)

fun DropshippingSaleDTO.toEntity(product: Product, merchant: Merchant)= DropshippingSale(id=this.id?:UUID.randomUUID(), product = product,
        merchant = merchant, amount=this.amount, specialConditions = this.specialConditions,
        creationDate = this.creationDate, disabled = this.disabled?:false, deletionDate = this.deletionDate
)

