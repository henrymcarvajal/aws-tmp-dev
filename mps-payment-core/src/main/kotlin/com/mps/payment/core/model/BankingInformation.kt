package com.mps.payment.core.model

import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

const val MERCHANT_ID_NOT_NULL = "Merchant id es obligatorio"
const val ACCOUNT_NUMBER_MANDATORY = "Número de cuenta es obligatorio"
const val DOCUMENT_NUMBER_INVALID = "Número de documento es obligatorio"
const val ACCOUNT_NUMBER_BAD_FORMAT = "Número de cuenta invalido"
const val DOCUMENT_TYPE_MANDATORY = "Tipo de documento es obligatorio"
const val DOCUMENT_NUMBER_MANDATORY = "Número de documento es obligatorio"
const val FULL_NAME_MANDATORY = "Nombre es obligatorio"
const val ACCOUNT_BANK_MANDATORY = "Banco de cuenta es obligatorio"
const val ACCOUNT_TYPE_MANDATORY = "Tipo de cuenta es obligatorio"

@Entity
data class BankingInformation(
        @Id val id: UUID, @Column(name = "merchant_id") val merchantId: UUID,
        @Column(name = "account_bank") var accountBank: Int, @Column(name = "account_type") var accountType: Int,
        @Column(name = "account_number") var accountNumber: Long, @Column(name = "document_type") var documentType: Int,
        @Column(name = "document_number") var documentNumber: Long, @Column(name = "full_name") var fullName: String
)

data class BankingInformationDTO(
        val id: UUID?, val merchantId: UUID,
        val accountBank: Int, val accountType: Int, val accountNumber: Long, val documentType: Int,
        val documentNumber: Long, val fullName: String
)

fun BankingInformation.toDTO() = BankingInformationDTO(
        id = this.id, merchantId = this.merchantId, accountBank = this.accountBank, accountType = this.accountType,
        accountNumber = this.accountNumber, documentType = this.documentType, documentNumber = this.documentNumber,
        fullName = this.fullName
)

fun BankingInformationDTO.toEntity() = BankingInformation(id = this.id
        ?: UUID.randomUUID(), merchantId = this.merchantId,
        accountBank = this.accountBank, accountNumber = this.accountNumber, accountType = this.accountType, documentNumber = this.documentNumber,
        documentType = this.documentType, fullName = this.fullName)

data class CreateBankingInformationRequest(
        val id: UUID?,
        @get:NotNull(message = MERCHANT_ID_NOT_NULL) val merchantId: UUID,
        @get:NotNull(message = ACCOUNT_BANK_MANDATORY) val accountBank: Int,
        @get:NotNull(message = ACCOUNT_TYPE_MANDATORY) val accountType: Int,
        @get:NotNull(message = ACCOUNT_NUMBER_MANDATORY) @get:Min(message = ACCOUNT_NUMBER_BAD_FORMAT,value = 999999999)  val accountNumber: Long,
        @get:NotNull(message = DOCUMENT_TYPE_MANDATORY) val documentType: Int,
        @get:NotNull(message = DOCUMENT_NUMBER_MANDATORY) @get:Min(message = DOCUMENT_NUMBER_INVALID,value = 999999) val documentNumber: Long,
        @get:NotNull(message = FULL_NAME_MANDATORY) @get:NotBlank(message = FULL_NAME_MANDATORY)  val fullName: String
)