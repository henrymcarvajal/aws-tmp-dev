package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.model.BankingInformation
import com.mps.payment.core.model.CreateBankingInformationRequest
import com.mps.payment.core.model.toDTO
import com.mps.payment.core.repository.BankingInformationRepository
import com.mps.payment.core.security.jwt.JwtTokenProvider
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BankingInformationService(private val merchantService: MerchantService,
                                private val bankingInformationRepository: BankingInformationRepository,
                                private val tokenProvider: JwtTokenProvider) {

    fun getBankInformationByMerchantId(merchantId: UUID, token: String): GenericResponse<*> {
        val userName = tokenProvider.getUsername(token)
        val optionalMerchant = merchantService.findById(merchantId)
        if (optionalMerchant.isEmpty) {
            return GenericResponse.ErrorResponse("Comercio no existe")
        }
        if (userName != optionalMerchant.get().email) {
            return GenericResponse.ErrorResponse("Usuario no autorizado")
        }
        return GenericResponse.SuccessResponse(bankingInformationRepository.findByMerchantId(merchantId))
    }

    fun createOrUpdateBankingInformation(createBakingInformationRequest: CreateBankingInformationRequest): GenericResponse<*> {
        val merchant = merchantService.getMerchant(createBakingInformationRequest.merchantId)
        if (merchant.isEmpty) {
            return GenericResponse.ErrorResponse(MERCHANT_NOT_EXIST)
        }
        var bankingInformation: BankingInformation =
                createOrReplaceBakingInformation(createBakingInformationRequest)?:
                return GenericResponse.ErrorResponse("No existe la información que desea actualizar")

        val createdBankingInfo = bankingInformationRepository.save(bankingInformation)

        return GenericResponse.SuccessResponse(createdBankingInfo.toDTO())

    }

    private fun createOrReplaceBakingInformation(createBakingInformationRequest: CreateBankingInformationRequest) =
            if (createBakingInformationRequest.id != null) {
                val existingBankingInformation = bankingInformationRepository.findById(createBakingInformationRequest.id)
                if (!existingBankingInformation.isEmpty) {
                    val existingBanking = existingBankingInformation.get()
                    existingBanking.accountBank = createBakingInformationRequest.accountBank
                    existingBanking.accountType = createBakingInformationRequest.accountType
                    existingBanking.fullName = createBakingInformationRequest.fullName
                    existingBanking.accountNumber = createBakingInformationRequest.accountNumber
                    existingBanking.documentNumber = createBakingInformationRequest.documentNumber
                    existingBanking.documentType = createBakingInformationRequest.documentType
                    existingBanking
                }else{
                    null
                }
            } else {
                BankingInformation(
                        id = UUID.randomUUID(), merchantId = createBakingInformationRequest.merchantId,
                        accountType = createBakingInformationRequest.accountType, accountBank = createBakingInformationRequest.accountBank,
                        accountNumber = createBakingInformationRequest.accountNumber, documentNumber = createBakingInformationRequest.documentNumber,
                        documentType = createBakingInformationRequest.documentType, fullName = createBakingInformationRequest.fullName
                )
            }
}