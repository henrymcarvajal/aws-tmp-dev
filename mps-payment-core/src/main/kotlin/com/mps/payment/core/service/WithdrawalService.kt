package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.PaymentDTO
import com.mps.payment.core.email.EmailSender
import com.mps.payment.core.model.*
import com.mps.payment.core.repository.BankingInformationRepository
import com.mps.payment.core.repository.PaymentRepository
import com.mps.payment.core.repository.WithdrawalRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID


const val TRANSFER_COMMISSION: Long = 6000
const val TEMPLATE_WITHDRAWAL_NOTIFY = "notificación de retiro"
const val MESSAGE_WITHDRAWAL_NOTIFICATION = "detalles del retiro en el siguiente enlace"

@Service
class WithdrawalService(private val withdrawalRepository: WithdrawalRepository,
                        private val merchantService: MerchantService,
                        private val paymentRepository: PaymentRepository,
                        private val emailSender: EmailSender,
                        private val bankingInformationRepository: BankingInformationRepository) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun createWithdrawal(withdrawalDTO: WithdrawalDTO): GenericResponse<*> {
        val merchant = merchantService.getMerchant(withdrawalDTO.idMerchant)
        if (merchant.isEmpty) {
            log.error("createWithdrawal: merchant does not exist")
            return GenericResponse.ErrorResponse("Comercio no existe")
        }
        if (merchantService.getAmountOfClosedPayments(withdrawalDTO.idMerchant) < withdrawalDTO.amount) {
            log.error("createWithdrawal: not enough money")
            return GenericResponse.ErrorResponse("Fondos insuficientes")
        }
        if (BigDecimal.valueOf(TRANSFER_COMMISSION) > withdrawalDTO.amount) {
            log.error("createWithdrawal: not enough money")
            return GenericResponse.ErrorResponse("Fondos insuficientes")
        }
        withdrawalDTO.amount = withdrawalDTO.amount - BigDecimal.valueOf(TRANSFER_COMMISSION)
        val createdWithdrawal = withdrawalRepository.save(withdrawalDTO.toEntity())
        merchantService.markAsTransferredClosedPayments(withdrawalDTO.idMerchant, createdWithdrawal.id)
        val account = bankingInformationRepository.findByMerchantId(withdrawalDTO.idMerchant)
        var accountNumber = ""
        var accountType = ""
        var accountBank = ""
        if(!account.isEmpty){
            accountNumber = account.get().accountNumber.toString()
            accountType = account.get().accountType.toString()
            accountBank= account.get().accountBank.toString()
        }
        emailSender.sendEmailWithTemplate(receiver = "operativo@mipagoseguro.co", templateName = TEMPLATE_EMAIL_PLANE_GENERIC, title = TEMPLATE_WITHDRAWAL_NOTIFY,
                o = mapOf(CONST_MESSAGE to "el comercio ${merchant.get().name} ha creado un retiro de valor $${withdrawalDTO.amount} con nit ${merchant.get().nit}," +
                        " número de cuenta bancaria ${accountNumber}, tipo de cuenta ${accountType}, " +
                        "del banco ${accountBank}.", "buttonText" to "Ver pago"))
        return GenericResponse.SuccessResponse(createdWithdrawal.toDTO())
    }

    fun getWithdrawalByService(idMerchant: UUID): List<Withdrawal> {
        return withdrawalRepository.findWithdrawalByIdMerchantByDesc(idMerchant)
    }

    fun getPaymentsByWithdrawal(idMerchant: UUID): List<PaymentDTO> {
        return paymentRepository.findPaymentsByWithdrawal(idMerchant).map { it.toDTO() }
    }
}