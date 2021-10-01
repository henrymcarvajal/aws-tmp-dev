package com.mps.payment.core.model

import com.mps.payment.core.service.TRANSFER_COMMISSION
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Withdrawal(
        @Id val id: UUID, @Column(name = "id_merchant") var idMerchant: UUID,
        @Column(name = "amount") val amount: BigDecimal,
        @Column(name = "application_date") val applicationDate: LocalDateTime= LocalDateTime.now(),
        @Column(name="comision") var comision:BigDecimal
)


data class WithdrawalDTO(
        val id: UUID?, var idMerchant: UUID,
        var amount: BigDecimal,
        val applicationDate: LocalDateTime?= LocalDateTime.now(),
        var comision:BigDecimal= BigDecimal.valueOf(TRANSFER_COMMISSION)
)

fun Withdrawal.toDTO() = WithdrawalDTO(
        id=this.id, idMerchant = this.idMerchant, amount = this.amount,comision = this.comision, applicationDate = this.applicationDate
)

fun WithdrawalDTO.toEntity() = Withdrawal(
        id = this.id?:UUID.randomUUID(), amount = this.amount, comision = this.comision, idMerchant = this.idMerchant,
        applicationDate = this.applicationDate ?: LocalDateTime.now()
)