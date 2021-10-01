package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.enum.AccountTypeEnum
import com.mps.payment.core.enum.BankEnum
import com.mps.payment.core.enum.PaymentStateEnum
import com.mps.payment.core.model.*
import com.mps.payment.core.repository.MerchantRepository
import com.mps.payment.core.repository.PaymentRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

const val MERCHANT_ID_EXISTS = "Merchant (id) already exists"
const val MANDATORY_FIELD_MISSING = "A mandatory field is missing"
const val MERCHANT_ALREADY_EXISTS = "Merchant (nit, email or contact number) already exists"
const val MERCHANT_NOT_EXISTS = "Merchant does not exist"
const val NIT_ALREADY_EXISTS = "Merchant (nit) already exists. Use put to update"
const val USER_ALREADY_EXISTS = "User (email) already exists"

@Service
class MerchantService(
        private val merchantRepository: MerchantRepository,
        private val userService: UserService,
        private val paymentRepository: PaymentRepository
) {

    @Transactional
    fun createMerchant(merchantDTO: MerchantDTO): GenericResponse<*> {
        if (merchantRepository.existsByEmail(merchantDTO.email)) {
            return GenericResponse.ErrorResponse(MERCHANT_ALREADY_EXISTS)
        }
        merchantDTO.id?.let {
            if (merchantRepository.existsById(it)) {
                return GenericResponse.ErrorResponse(MERCHANT_ID_EXISTS)
            }
        }
        merchantDTO.nit?.let {
            merchantRepository.findMerchantByNit(merchantDTO.nit)?.let {
                return GenericResponse.ErrorResponse(NIT_ALREADY_EXISTS)
            }
        }

        val merchant = merchantDTO.toEntity()
        merchant.creationDate = LocalDateTime.now()

        val user = createUser(merchantDTO)
        if (user != null) {
           merchant.id = user.id
           merchant.idUser = user.id
        }else{
           return GenericResponse.ErrorResponse(USER_ALREADY_EXISTS)
        }

        val newMerchant = merchantRepository.save(merchant).toDto()
        return GenericResponse.SuccessResponse(newMerchant)
    }

    @Transactional
    fun updateMerchant(merchantDTO: MerchantDTO): GenericResponse<*> {
        if(merchantDTO.id==null){
            return GenericResponse.ErrorResponse(MANDATORY_FIELD_MISSING)
        }
        val merchant = merchantRepository.findById(merchantDTO.id!!)
        return if(merchant.isEmpty){
             GenericResponse.ErrorResponse(MERCHANT_NOT_EXISTS)
        }else{
            val merchantValue = merchant.get()
            if(merchantValue.nit==null){
                merchantValue.nit=merchantDTO.nit
            }
            merchantValue.contactNumber=merchantDTO.contactNumber.toLong()
            val newMerchant = merchantRepository.save(merchantValue).toDto()
            GenericResponse.SuccessResponse(newMerchant)
        }
    }

    @Transactional
    fun updatePixel(pixelInfo: Map<String,String>): GenericResponse<*> {
        val id = pixelInfo["id"] ?: return GenericResponse.ErrorResponse(MANDATORY_FIELD_MISSING)
        val pixel = pixelInfo["fbId"] ?: return GenericResponse.ErrorResponse(MANDATORY_FIELD_MISSING)
        val merchant = merchantRepository.findById(UUID.fromString(id))
        return if(merchant.isEmpty){
            GenericResponse.ErrorResponse(MERCHANT_NOT_EXISTS)
        }else{
            val merchantValue = merchant.get()
            merchantValue.fbPixel = pixel
            val newMerchant = merchantRepository.save(merchantValue).toDto()
            GenericResponse.SuccessResponse(newMerchant)
        }
    }

    private fun createUser(merchantDTO: MerchantDTO) : User? {
        val user = User(null,merchantDTO.name.trim().toLowerCase(), merchantDTO.email.trim().toLowerCase(),
                merchantDTO.password?:null, "ROLE_MERCHANT")
        return userService.create(user)
    }

    fun getMerchant(id:UUID) = merchantRepository.findById(id)

    fun existsById(id:UUID) = merchantRepository.existsById(id)

    fun getAmountOfClosedPayments(idMerchant:UUID):BigDecimal{
        val closedPayments = paymentRepository.findPaymentByIdMerchantAndState(idMerchant,PaymentStateEnum.CLOSED.state)
        return if(closedPayments.isEmpty()){
           BigDecimal.ZERO
        }else{
            closedPayments.fold(BigDecimal.ZERO,{acc, payment -> acc + payment.amount-payment.comision!! })
        }
    }

    @Transactional
    fun markAsTransferredClosedPayments(idMerchant:UUID, withdrawalId:UUID){
        val closedPayments = paymentRepository.findPaymentByIdMerchantAndState(idMerchant,PaymentStateEnum.CLOSED.state)
        closedPayments.forEach{
            it.idState = PaymentStateEnum.TRANSFERRED.state
            it.withdrawal = withdrawalId
            paymentRepository.save(it)
        }
    }

    fun findById(id: UUID) = merchantRepository.findById(id)

    fun findByIdOrNull(id: UUID) = merchantRepository.findByIdOrNull(id)

    fun findByEmail(email: String) = merchantRepository.findByEmail(email)
}