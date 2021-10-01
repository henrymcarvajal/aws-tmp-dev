package com.mps.payment.core.model

import com.mps.common.dto.PaymentDTO
import com.mps.payment.core.enum.PaymentStateEnum

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull


@Entity
data class Payment(@Id val id: UUID, @Column(name = "id_merchant") var idMerchant: UUID,
                   @Column(name = "id_customer") var idCustomer: UUID?,
                   val amount: BigDecimal,
                   @Column(name = "id_payment_state") var idState: Int, @Column(name = "created_at") var creationDate: LocalDateTime?= LocalDateTime.now(),
                   @Column(name = "last_updated") val modificationDate: LocalDateTime, @Column(name = "link_Url") var linkUrl: String?=null,
                   @Column(name = "comision") var comision: BigDecimal?= BigDecimal.ZERO,
                   @Column(name = "description") val description: String?= null,
                   @Column(name = "guide_number") var guideNumber: String?=null,
                   @Column(name = "transport_company") var transportCompany: String?=null,
                   @Column(name = "close_date") var closeDate: LocalDateTime? = null,
                   @Column(name = "withdrawal") var withdrawal: UUID?= null,
                   @Column(name = "product_id") var productId: UUID?= null
)

data class PaymentWithCustomerDTO( val id: UUID?,
                      val description: String?,
                      @get:NotNull(message = "id Merchant can not be null") val idMerchant: UUID?,
                      val idCustomer: UUID?,
                      var customerName: String?,
                      @get:NotNull(message = "amount can not be null") @get:Min(value = 20000,
                              message = "El valor de la transacción no puede ser menor a $20.000") var amount: BigDecimal?,
                      val creationDate: LocalDateTime?=null,
                      val modificationDate: LocalDateTime?=null,
                      var idState: Int?, val guideNumber: String?, val transportCompany: String?,
                      var linkUrl: String?,
                        val publicId:String?=null)

data class DelayCloseDateToPaymentInput(
        @get:NotNull(message = "payment id is mandatory") val id:UUID,
        @get:NotNull(message = "identification is mandatory") val identificationCustomer:String
)

data class PaymentStatePublicInput(
        @get:NotNull(message = "payment id is mandatory") val paymentId:String?,
        @get:NotNull(message = "cédula es obligatoria") val numberId:String?,
        @get:Min(1,message = "invalid state")
        val state:Int
)

data class QueryParams(
        val duration:Int, val state:Int,val guideNumber: Int?=null,val customerNumberContact:Long?=null,
        val pageNumber:Int=1
)

fun Payment.toDTO() =
        PaymentDTO(
                id = this.id, idCustomer = this.idCustomer, idMerchant = this.idMerchant, amount = this.amount, idState = this.idState,
                creationDate = this.creationDate, modificationDate = this.modificationDate, linkUrl = this.linkUrl,
                guideNumber = this.guideNumber, transportCompany = this.transportCompany, description = this.description,
                closeDate = this.closeDate, withdrawal = this.withdrawal, comision = this.comision, publicId =
        this.id?.toString().takeLast(6), productId = this.productId
        )

fun Payment.toWithCustomerDTO() =
        PaymentWithCustomerDTO(
                id= this.id,
                description = this.description, idCustomer = this.idCustomer, customerName = null, idMerchant = this.idMerchant, amount = this.amount, idState = this.idState,
                creationDate = this.creationDate, modificationDate = this.modificationDate,
                linkUrl = this.linkUrl,guideNumber = this.guideNumber,transportCompany = this.transportCompany,
                publicId = this.id?.toString().takeLast(6)
        )

fun PaymentDTO.toEntity() =
        Payment(id = this.id
                ?: UUID.randomUUID(), idCustomer = this.idCustomer, idMerchant = this.idMerchant!!, amount = this.amount!!,
                idState = this.idState?: PaymentStateEnum.INITIAL.state,
                modificationDate = LocalDateTime.now(), linkUrl = this.linkUrl,
                guideNumber = this.guideNumber, transportCompany = this.transportCompany, description = this.description,
                closeDate = this.closeDate, withdrawal = this.withdrawal, comision = this.comision,
                productId = this.productId
        )

