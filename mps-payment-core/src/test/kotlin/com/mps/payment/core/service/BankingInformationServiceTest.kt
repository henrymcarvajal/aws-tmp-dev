package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.model.toEntity
import com.mps.payment.core.repository.BankingInformationRepository
import com.mps.payment.core.security.jwt.JwtTokenProvider
import com.mps.payment.core.util.anyObject
import com.mps.payment.core.util.createBankingInformation
import com.mps.payment.core.util.createMerchantTest
import com.mps.payment.core.util.createUpdateBankingInformationRequest
import io.jsonwebtoken.lang.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.UUID
import java.util.Optional

internal class BankingInformationServiceTest{

    @Mock
    private lateinit var merchantService: MerchantService

    @Mock
    private lateinit var bankingInformationRepository: BankingInformationRepository

    @Mock
    private lateinit var tokenProvider: JwtTokenProvider

    private lateinit var bankingInformationService: BankingInformationService


    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        bankingInformationService =
                BankingInformationService(merchantService, bankingInformationRepository,tokenProvider)
    }
    @Test
    fun `merchant is empty`(){
        val merchantId = UUID.randomUUID()
        Mockito.`when`(merchantService.getMerchant(merchantId)).thenReturn(Optional.empty())
        val request = createUpdateBankingInformationRequest(merchantId)
        val response = bankingInformationService.createOrUpdateBankingInformation(request)
        Assert.isTrue(response is GenericResponse.ErrorResponse)
    }

    @Test
    fun `update case of unexisting banking record`(){
        val merchantId = UUID.randomUUID()
        val id = UUID.randomUUID()
        Mockito.`when`(merchantService.getMerchant(merchantId)).thenReturn(Optional.of(createMerchantTest().toEntity()))
        Mockito.`when`(bankingInformationRepository.findById(id)).thenReturn(Optional.empty())
        val request = createUpdateBankingInformationRequest(merchantId,id)
        val response = bankingInformationService.createOrUpdateBankingInformation(request)
        Assert.isTrue(response is GenericResponse.ErrorResponse)
    }

    @Test
    fun `update case of successful`(){
        val merchantId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val bankingInformation =createBankingInformation()
        Mockito.`when`(merchantService.getMerchant(merchantId)).thenReturn(Optional.of(createMerchantTest().toEntity()))
        Mockito.`when`(bankingInformationRepository.findById(id)).thenReturn(Optional.of(bankingInformation))
        Mockito.`when`(bankingInformationRepository.save(bankingInformation)).thenReturn(bankingInformation)
        val request = createUpdateBankingInformationRequest(merchantId,id)
        val response = bankingInformationService.createOrUpdateBankingInformation(request)
        Assert.isTrue(response is GenericResponse.SuccessResponse)
    }

    @Test
    fun `create case of successful`(){
        val merchantId = UUID.randomUUID()
        val bankingInformation =createBankingInformation()
        Mockito.`when`(merchantService.getMerchant(merchantId)).thenReturn(Optional.of(createMerchantTest().toEntity()))
        Mockito.`when`(bankingInformationRepository.save(anyObject())).thenReturn(bankingInformation)
        val request = createUpdateBankingInformationRequest(merchantId)
        val response = bankingInformationService.createOrUpdateBankingInformation(request)
        Assert.isTrue(response is GenericResponse.SuccessResponse)
    }

    @Test
    fun `get banking info merchant does not exist`(){
        val merchantId = UUID.randomUUID()
        Mockito.`when`(merchantService.findById(merchantId)).thenReturn(Optional.empty())
        val response=bankingInformationService.getBankInformationByMerchantId(merchantId,"")
        Assert.isTrue(response is GenericResponse.ErrorResponse)
    }

    @Test
    fun `get banking info merchant unauthorized`(){
        val merchantId = UUID.randomUUID()
        val token = "token"
        Mockito.`when`(merchantService.findById(merchantId)).thenReturn(Optional.of(createMerchantTest(merchantId).toEntity()))
        Mockito.`when`(tokenProvider.getUsername(token)).thenReturn("not match")
        val response=bankingInformationService.getBankInformationByMerchantId(merchantId,token)
        Assert.isTrue(response is GenericResponse.ErrorResponse)
    }

    @Test
    fun `get banking info merchant successful`(){
        val merchantId = UUID.randomUUID()
        val token = "token"
        Mockito.`when`(merchantService.findById(merchantId)).thenReturn(Optional.of(createMerchantTest(merchantId).toEntity()))
        Mockito.`when`(tokenProvider.getUsername(token)).thenReturn("jorjek4@hotmail.com")
        Mockito.`when`(bankingInformationRepository.findByMerchantId(merchantId)).thenReturn(Optional.of(createBankingInformation(merchantId)))
        val response=bankingInformationService.getBankInformationByMerchantId(merchantId,token)
        Assert.isTrue(response is GenericResponse.SuccessResponse)
    }
}