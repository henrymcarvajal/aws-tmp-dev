package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.email.EmailSender
import com.mps.payment.core.model.Withdrawal
import com.mps.payment.core.model.toEntity
import com.mps.payment.core.repository.BankingInformationRepository
import com.mps.payment.core.repository.PaymentRepository
import com.mps.payment.core.repository.WithdrawalRepository
import com.mps.payment.core.util.createMerchantTest
import com.mps.payment.core.util.createWithdrawalTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.springframework.util.Assert
import java.math.BigDecimal
import java.util.*


internal class WithdrawalServiceTest {

    @Mock
    private lateinit var withdrawalRepository: WithdrawalRepository

    @Mock
    private lateinit var merchantService: MerchantService

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @Mock
    private lateinit var emailSender: EmailSender

    @Mock
    private lateinit var bankingInformationRepository: BankingInformationRepository

    private lateinit var withdrawalService: WithdrawalService

    @BeforeEach
    fun setup(){
        MockitoAnnotations.initMocks(this)
        withdrawalService = WithdrawalService(withdrawalRepository, merchantService,paymentRepository,emailSender,bankingInformationRepository)
    }

    @Test
    fun `try to create withdrawal of non existing merchant`(){
        val merchantId = UUID.randomUUID()
        Mockito.`when`(merchantService.getMerchant(merchantId)).thenReturn(Optional.empty())
        val responseService = withdrawalService.createWithdrawal(createWithdrawalTest(merchantId))
        Assert.isInstanceOf(GenericResponse.ErrorResponse::class.java, responseService)
        Assert.isTrue("Comercio no existe" == responseService.objP as String,"Comercio no existe")
    }

    @Test
    fun `try to create withdrawal of lower amount than commission`(){
        val merchantId = UUID.randomUUID()
        Mockito.`when`(merchantService.getMerchant(merchantId)).thenReturn(Optional.of(createMerchantTest().toEntity()))
        Mockito.`when`(merchantService.getAmountOfClosedPayments(merchantId)).thenReturn(BigDecimal.valueOf(100000))
        val responseService = withdrawalService.createWithdrawal(createWithdrawalTest(merchantId, BigDecimal.valueOf(5000)))
        Assert.isInstanceOf(GenericResponse.ErrorResponse::class.java, responseService)
        Assert.isTrue("Fondos insuficientes" == responseService.objP as String,"Fondos insuficientes")
    }

    @Test
    fun `try to create withdrawal of higher amount than available closed payments`(){
        val merchantId = UUID.randomUUID()
        Mockito.`when`(merchantService.getMerchant(merchantId)).thenReturn(Optional.of(createMerchantTest().toEntity()))
        Mockito.`when`(merchantService.getAmountOfClosedPayments(merchantId)).thenReturn(BigDecimal.valueOf(100000))
        val responseService = withdrawalService.createWithdrawal(createWithdrawalTest(merchantId, BigDecimal.valueOf(110000)))
        Assert.isInstanceOf(GenericResponse.ErrorResponse::class.java, responseService)
        Assert.isTrue("Fondos insuficientes" == responseService.objP as String,"Fondos insuficientes")
    }

    @Test
    fun `happy path`(){
        val merchantId = UUID.randomUUID()
        val merchant = createMerchantTest().toEntity()
        val withdrawal = createWithdrawalTest(merchantId, BigDecimal.valueOf(100000))
        val withdrawalEntity = withdrawal.toEntity()
        Mockito.`when`(merchantService.getMerchant(merchantId)).thenReturn(Optional.of(merchant))
        Mockito.`when`(merchantService.getAmountOfClosedPayments(merchantId)).thenReturn(BigDecimal.valueOf(100000))
        Mockito.`when`(withdrawalRepository.save(Mockito.any<Withdrawal>())).thenReturn(withdrawalEntity)
        val responseService = withdrawalService.createWithdrawal(withdrawal)
        Assert.isInstanceOf(GenericResponse.SuccessResponse::class.java, responseService)
    }

}