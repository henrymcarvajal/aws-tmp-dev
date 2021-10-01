package com.mps.payment.core.model

import com.mps.common.dto.OrderDropDTO
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name="general_order_drop")
data class GeneralOrderDrop(@Id val id: UUID,
                            @Column(name = "customer_id") var customerId: UUID,
                            @Column(name = "order_status") var orderStatus: Int,
                            @Column(name = "created_at") var creationDate: LocalDateTime= LocalDateTime.now(),
                            @Column(name = "last_updated") val modificationDate: LocalDateTime,
                            @Column(name = "comision") var comision: BigDecimal= BigDecimal.ZERO,
                            @Column(name = "guide_number") var guideNumber: Int?=null,
                            @OneToOne(cascade = [CascadeType.ALL]) @JoinColumn(name = "product_id", referencedColumnName = "id") var dropShippingSale: DropshippingSale,
                            @Column(name = "payment_method") var paymentMethod: String,
                            @Column(name = "payment_id") var paymentId: String?= null,
                            @Column(name = "quantity") var quantity: Int,
                            @Column(name = "amount") var amount: BigDecimal,
                            @Column(name = "observations") var observations: String="",
                            @Column(name = "freight_price") var freightPrice: BigDecimal?,
                            @Column(name = "branch_code") var branchCode: Int,
                            @Column(name = "is_labeled") var isLabeled: Boolean,
                            @Column(name = "label_url") var label: String
)

fun OrderDropDTO.toEntity(orderStatusInput: Int=0, dropShippingSale: DropshippingSale,customerId:UUID)= GeneralOrderDrop(
        id = this.id?: UUID.randomUUID(), orderStatus = this.orderStatus?:orderStatusInput, creationDate = this.creationDate?: LocalDateTime.now(),
        modificationDate = this.modificationDate?: LocalDateTime.now(),comision = this.comision?: BigDecimal.ZERO, guideNumber = this.guideNumber,
        dropShippingSale = dropShippingSale,paymentMethod = this.paymentMethod,
        customerId=customerId, quantity = this.quantity, paymentId = this.paymentId, amount = this.amount,
        freightPrice =this.freightPrice, observations = this.observations,branchCode = this.branchCode,
        isLabeled = this.isLabeled,label = this.label?:""
)

fun GeneralOrderDrop.toDTO()= OrderDropDTO(
        id = this.id, orderStatus = this.orderStatus, creationDate = this.creationDate,
        modificationDate = this.modificationDate,comision = this.comision, guideNumber = this.guideNumber,
        productId = this.dropShippingSale.id,paymentMethod = this.paymentMethod,
        customer= null, quantity = this.quantity, paymentId = this.paymentId, amount = this.amount,
        freightPrice = this.freightPrice, observations = this.observations,
        branchCode = this.branchCode,isLabeled = this.isLabeled,sellerName=this.dropShippingSale.merchant.name,
        label = this.label
)

data class OrderViewDropshipper(
        val buyPrice:BigDecimal,
        val profitSale: BigDecimal,
        val customerPhone:String,
        override val orderId: UUID,
        override val productName: String,
        override val customerName: String,
        override val sellPrice: BigDecimal,
        override val orderState: Int,
        override val guideNumber: Int?,
        override val creationDate: LocalDateTime,
        override val freightPrice: BigDecimal
):OrderSummary


data class OrderViewProvider(
        override val orderId: UUID,
        override val productName: String,
        override val customerName: String,
        override val sellPrice: BigDecimal,
        override val orderState: Int,
        override val guideNumber: Int?,
        override val creationDate: LocalDateTime,
        override val freightPrice: BigDecimal,
        val isLabeled: Boolean
):OrderSummary

interface  OrderSummary {
    val orderId: UUID
    val productName: String
    val customerName: String
    val sellPrice: BigDecimal
    val orderState: Int
    val guideNumber: Int?
    val creationDate: LocalDateTime
    val freightPrice: BigDecimal
}


data class OrderViewConsolidate(
    val orders: List<OrderSummary>,
    val totalProfitSaleByStatus: BigDecimal,
    val totalOrderByStatus: Int,
    val totalOrderAmountByStatus:BigDecimal,
    val actualPage:Int,
    val totalRecords:Int,
    val totalPages:Int
)

data class OrderConsolidateGroupByStatus(
        val status: Int,
        val quantity: Int,
        val totalAmount: BigDecimal,
        val profit: BigDecimal?
)