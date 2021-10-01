package com.mps.payment.core.model

import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Branch(
        @Id var id: UUID,
        var name: String,
        var address: String,
        var daneCodeId: UUID,
        var merchantId: UUID,
        var contactName: String,
        var contactIdentification: Long,
        var contactEmail: String,
        var contactPhone: Long,
        @Column(name = "created_at") var creationDate: LocalDateTime? = LocalDateTime.now(),
        @Column(name = "last_updated") var modificationDate: LocalDateTime? = LocalDateTime.now(),
        var disabled: Boolean = false,
        @Column(name = "disabled_at") var deletionDate: LocalDateTime? = LocalDateTime.now(),
        @Column(name = "branch_code") var branchCode: Int
) {
    companion object {
        const val ADDRESS = "address"
        const val DANE_CODE = "daneCodeId"
        const val MERCHANT_ID = "merchantId"
        const val DISABLED = "disabled"
    }
}

data class BranchDTO(
        var id: UUID?,
        val name: String,
        val address: String,
        var daneCodeId: UUID,
        var merchantId: UUID?,
        var contactName: String,
        var contactIdentification: Long,
        var contactEmail: String,
        var contactPhone: Long,
        var creationDate: LocalDateTime? = LocalDateTime.now(),
        var modificationDate: LocalDateTime? = LocalDateTime.now(),
        var disabled: Boolean = false,
        var deletionDate: LocalDateTime? = null,
        var branchCode: Int=0
)

fun Branch.toDTO() = BranchDTO(
        id = this.id, name = this.name,
        address = this.address,
        daneCodeId = this.daneCodeId,
        merchantId = this.merchantId,
        contactName = this.contactName,
        contactIdentification = this.contactIdentification,
        contactEmail = this.contactEmail,
        contactPhone = this.contactPhone,
        creationDate = this.creationDate,
        modificationDate = this.modificationDate,
        disabled = this.disabled,
        deletionDate = this.deletionDate,
        branchCode = this.branchCode
)

fun BranchDTO.toEntity() = Branch(
        id = this.id ?: UUID.randomUUID(),
        name = this.name,
        address = this.address,
        daneCodeId = this.daneCodeId,
        merchantId = this.merchantId ?: UUID.randomUUID(),
        contactName = this.contactName,
        contactIdentification = this.contactIdentification,
        contactEmail = this.contactEmail,
        contactPhone = this.contactPhone,
        creationDate = this.creationDate,
        modificationDate = this.modificationDate,
        disabled = this.disabled,
        deletionDate = this.deletionDate,
        branchCode = this.branchCode
)