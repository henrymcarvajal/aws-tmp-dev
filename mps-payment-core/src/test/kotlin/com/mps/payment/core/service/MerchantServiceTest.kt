package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.model.Merchant
import com.mps.payment.core.model.User
import com.mps.payment.core.model.toEntity
import com.mps.payment.core.repository.MerchantRepository
import com.mps.payment.core.repository.PaymentRepository
import com.mps.payment.core.util.createMerchantTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.springframework.util.Assert
import java.util.*

internal class MerchantServiceTest{

    @Mock
    private lateinit var merchantRepositoryMock: MerchantRepository

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @Mock
    private lateinit var userServiceMock: UserService

    private lateinit var merchantService: MerchantService

    @BeforeEach
    fun setup(){
        MockitoAnnotations.initMocks(this)
        merchantService = MerchantService(merchantRepositoryMock, userServiceMock,paymentRepository)
    }

    @Test
    fun `try to create existing merchant`(){
        Mockito.`when`(merchantRepositoryMock.existsById(Mockito.any())).thenReturn(true)
        val responseService = merchantService.createMerchant(createMerchantTest())
        Assert.isInstanceOf(GenericResponse.ErrorResponse::class.java, responseService)
        Assert.isTrue(MERCHANT_ID_EXISTS == responseService.objP as String,"message should be equals")
    }

    @Test
    fun `try to create merchant with existing nit`(){
        val nit = "1234567"
        val merchantTest = createMerchantTest(nit= nit)
        Mockito.`when`(merchantRepositoryMock.findMerchantByNit(nit)).thenReturn(merchantTest.toEntity())
        Mockito.`when`(merchantRepositoryMock.existsById(Mockito.any())).thenReturn(false)
        val responseService = merchantService.createMerchant(merchantTest)
        Assert.isInstanceOf(GenericResponse.ErrorResponse::class.java, responseService)
        Assert.isTrue(NIT_ALREADY_EXISTS == responseService.objP as String,"message should be equals")
    }

    @Test
    fun `create merchant happy path`(){
        val nit = "1234567"
        val merchantTest = createMerchantTest(nit= nit)
        val user = User(null,merchantTest.name, merchantTest.email, merchantTest.password!!, "ROLE_MERCHANT")
        Mockito.`when`(merchantRepositoryMock.findMerchantByNit(nit)).thenReturn(null)
        Mockito.`when`(merchantRepositoryMock.existsById(Mockito.any())).thenReturn(false)
        Mockito.`when`(userServiceMock.create(user)).thenReturn(User(id= UUID.randomUUID(),name="name",username = "username",password = "pass",roles = "admin"))
        Mockito.`when`(merchantRepositoryMock.save(Mockito.any<Merchant>())).thenReturn(merchantTest.toEntity())
        val responseService = merchantService.createMerchant(merchantTest)
        Assert.isInstanceOf(GenericResponse.SuccessResponse::class.java, responseService)
    }

    @Test
    fun `update merchant happy path`(){
        val id = UUID.randomUUID()
        val merchantTest = createMerchantTest(id= id)
        Mockito.`when`(merchantRepositoryMock.findById(id)).thenReturn(Optional.of(merchantTest.toEntity()))
        Mockito.`when`(merchantRepositoryMock.save(Mockito.any<Merchant>())).thenReturn(merchantTest.toEntity())
        val responseService = merchantService.updateMerchant(merchantTest)
        Assert.isInstanceOf(GenericResponse.SuccessResponse::class.java, responseService)
    }

    @Test
    fun `update merchant id null`(){
        val merchantTest = createMerchantTest(id= null)
        val responseService = merchantService.updateMerchant(merchantTest)
        Assert.isInstanceOf(GenericResponse.ErrorResponse::class.java, responseService)
        Assert.isTrue(MANDATORY_FIELD_MISSING == responseService.objP as String,"message should be equals")
    }

    @Test
    fun `update when merchant does not exist`(){
        val id = UUID.randomUUID()
        val merchantTest = createMerchantTest(id= id)
        Mockito.`when`(merchantRepositoryMock.findById(id)).thenReturn(Optional.empty())
        val responseService = merchantService.updateMerchant(merchantTest)
        Assert.isInstanceOf(GenericResponse.ErrorResponse::class.java, responseService)
        Assert.isTrue(MERCHANT_NOT_EXISTS == responseService.objP as String,"message should be equals")
    }
}