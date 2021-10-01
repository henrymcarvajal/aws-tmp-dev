package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.PaymentDTO
import com.mps.common.dto.PaymentStateInput
import com.mps.payment.core.email.EmailSender
import com.mps.payment.core.enum.PaymentStateEnum
import com.mps.payment.core.model.*
import com.mps.payment.core.repository.PaymentRepository
import com.mps.payment.core.util.createMerchantTest
import com.mps.payment.core.util.createPaymentAgreeTest
import com.mps.payment.core.util.createPaymentTest
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.BeforeEach
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.springframework.core.env.Environment
import org.springframework.util.Assert
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

internal class PaymentServiceTest {

    @Mock
    private lateinit var paymentRepositoryMock: PaymentRepository

    @Mock
    private lateinit var merchantServiceMock: MerchantService

    @Mock
    private lateinit var customerServiceMock: CustomerService

    @Mock
    private lateinit var emailSender: EmailSender

    private lateinit var paymentService: PaymentService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        paymentService = PaymentService(paymentRepositoryMock, merchantServiceMock,
                customerServiceMock, emailSender)
        paymentService.url = "url"
    }

    @Test
    fun `create Payment for existing record`() {
        Mockito.`when`(paymentRepositoryMock.existsById(Mockito.any())).thenReturn(true)
        val responseService = paymentService.createPayment(createPaymentTest())
        Assert.isInstanceOf(GenericResponse.ErrorResponse::class.java, responseService)
        Assert.isTrue(EXISTING_MESSAGE_ERROR == responseService.objP as String, "message should be equals")
    }

    @Test
    fun `create payment merchant does not exist`() {
        val idMerchant = UUID.randomUUID()
        Mockito.`when`(merchantServiceMock.existsById(idMerchant)).thenReturn(false)
        val responseService = paymentService.createPayment(createPaymentTest(merchantId = idMerchant))
        Assert.isInstanceOf(GenericResponse.ErrorResponse::class.java, responseService)
        Assert.isTrue(MERCHANT_NOT_EXIST == responseService.objP as String, "message should be equals")
    }

    @Test
    fun `create payment happy path`() {
        val id = UUID.randomUUID()
        val idMerchant = UUID.randomUUID()
        val paymentToSave = createPaymentTest(idMerchant, id)
        Mockito.`when`(merchantServiceMock.existsById(idMerchant)).thenReturn(true)
        Mockito.`when`(paymentRepositoryMock.existsById(id)).thenReturn(false)
        Mockito.`when`(paymentRepositoryMock.save(Mockito.any<Payment>())).thenReturn(paymentToSave.toEntity())
        Mockito.`when`(merchantServiceMock.getMerchant(idMerchant)).thenReturn(Optional.of(createMerchantTest().toEntity()))
        val responseService = paymentService.createPayment(paymentToSave)
        Assert.isInstanceOf(GenericResponse.SuccessResponse::class.java, responseService)
    }

    @Test
    fun `agree payment payment does not exists`() {
        val paymentId = UUID.randomUUID().toString()
        Mockito.`when`(paymentRepositoryMock.getPaymentByShortId(paymentId)).thenReturn(emptyList())
        val responseService = paymentService.agreePayment(createPaymentAgreeTest(paymentId = paymentId))
        Assert.isInstanceOf(GenericResponse.ErrorResponse::class.java, responseService)
        Assert.isTrue(PAYMENT_NOT_EXIST == responseService.objP as String, "message should be equals")
    }

    @Test
    fun `agree payment payment incorrect state`() {
        val paymentId = UUID.randomUUID().toString().takeLast(6)
        val payment = createPaymentTest()
        payment.idState = PaymentStateEnum.CLOSED.state
        Mockito.`when`(paymentRepositoryMock.getPaymentByShortId(paymentId)).thenReturn(listOf<Payment>(payment.toEntity()))
        val responseService = paymentService.agreePayment(createPaymentAgreeTest(paymentId = paymentId))
        Assert.isInstanceOf(GenericResponse.ErrorResponse::class.java, responseService)
        Assert.isTrue(PAYMENT_INCORRECT_STATE == responseService.objP as String, "message should be equals")
    }

    @Test
    fun `agree payment happy path`() {
        val paymentId = UUID.randomUUID().toString().takeLast(6)
        val payment = createPaymentTest()
        val paymentAgree = createPaymentAgreeTest(paymentId = paymentId)
        payment.idState = PaymentStateEnum.INITIAL.state
        Mockito.`when`(paymentRepositoryMock.getPaymentByShortId(paymentId)).thenReturn(listOf<Payment>(payment.toEntity()))
        Mockito.`when`(customerServiceMock.createOrReplaceCustomer(paymentAgree.customer)).thenReturn(paymentAgree.customer.toEntity())
        Mockito.`when`(paymentRepositoryMock.save(Mockito.any<Payment>())).thenReturn(payment.toEntity())
        val responseService = paymentService.agreePayment(paymentAgree)
        Assert.isInstanceOf(GenericResponse.SuccessResponse::class.java, responseService)
    }

    @Test
    fun `update state of not existence payment`() {
        Mockito.`when`(paymentRepositoryMock.existsById(Mockito.any())).thenReturn(false)
        val responseService = paymentService.updateStateOfPayment(PaymentStateInput(paymentId = UUID.randomUUID().toString(), state = 2))
        Assert.isInstanceOf(GenericResponse.ErrorResponse::class.java, responseService)
        Assert.isTrue(PAYMENT_NOT_EXIST == responseService.objP as String, "message should be equals")
    }

    @Test
    fun `update state using invalid state`() {
        Mockito.`when`(paymentRepositoryMock.existsById(Mockito.any())).thenReturn(true)
        val responseService = paymentService.updateStateOfPayment(PaymentStateInput(paymentId = UUID.randomUUID().toString(), state = 9))
        Assert.isInstanceOf(GenericResponse.ErrorResponse::class.java, responseService)
        Assert.isTrue(STATE_NOT_VALID == responseService.objP as String, "message should be equals")
    }

    @Test
    fun `update state happy path`() {
        Mockito.`when`(paymentRepositoryMock.existsById(Mockito.any())).thenReturn(true)
        Mockito.`when`(paymentRepositoryMock.findById(Mockito.any())).thenReturn(Optional.of(createPaymentTest().toEntity()))
        val responseService = paymentService
                .updateStateOfPayment(PaymentStateInput(paymentId = UUID.randomUUID().toString(), state = 4,guideNumber="1234",
                        transportCompany = "senderDelivery"))
        Assert.isInstanceOf(GenericResponse.SuccessResponse::class.java, responseService)
    }

    @Test
    fun `calculate fee, first month of sells`(){
        val oldDate = LocalDateTime.now().minusDays(6)
        val merchant = createMerchantTest().toEntity()
        merchant.creationDate = oldDate
        val method = paymentService.javaClass.getDeclaredMethod("calculateFee", Merchant::class.java, BigDecimal::class.java)
        method.isAccessible = true
        val parameters = arrayOfNulls<Any>(2)
        parameters[0] = merchant
        parameters[1] = BigDecimal.valueOf(100000)
        val fee=method.invoke(paymentService,*parameters) as BigDecimal
        Assert.isTrue((BigDecimal.valueOf(3700)-fee)< BigDecimal.ONE,"fee is not equal")
    }

    @Test
    fun `cashin scenarios payment does not exist`(){
        val paymentId = "123456"
        Mockito.`when`(paymentRepositoryMock.getPaymentByShortId(paymentId)).thenReturn(listOf())
        Mockito.`when`(paymentRepositoryMock.findById(Mockito.any())).thenReturn(Optional.of(createPaymentTest().toEntity()))
        val responseService = paymentService
                .updateStateOfPayment(PaymentStateInput(paymentId = paymentId, state = 4,guideNumber="1234",
                        transportCompany = "senderDelivery"))
        Assert.isInstanceOf(GenericResponse.ErrorResponse::class.java, responseService)
        Assert.isTrue(PAYMENT_NOT_EXIST==responseService.objP, "message does not match")
    }

    @Test
    fun `cashin scenarios happy path`(){
        val paymentId = "123456"
        val payment = createPaymentTest().toEntity()
        Mockito.`when`(paymentRepositoryMock.getPaymentByShortId(paymentId)).thenReturn(listOf(payment))
        Mockito.`when`(paymentRepositoryMock.findById(Mockito.any())).thenReturn(Optional.of(payment))
        val responseService = paymentService
                .updateStateOfPayment(PaymentStateInput(paymentId = paymentId, state = 4,guideNumber="1234",
                        transportCompany = "senderDelivery"))
        Assert.isInstanceOf(GenericResponse.SuccessResponse::class.java, responseService)
        Assert.isTrue(payment.id==(responseService.objP as PaymentDTO).id, "payment comparison does not match")
    }

    @Test
    fun `cashin scenarios multiple payments per id`(){
        val paymentId = "123456"
        val payment = createPaymentTest().toEntity()
        Mockito.`when`(paymentRepositoryMock.getPaymentByShortId(paymentId)).thenReturn(listOf(payment,payment))
        Mockito.`when`(paymentRepositoryMock.findById(Mockito.any())).thenReturn(Optional.of(payment))
        val responseService = paymentService
                .updateStateOfPayment(PaymentStateInput(paymentId = paymentId, state = 4,guideNumber="1234",
                        transportCompany = "senderDelivery"))
        Assert.isInstanceOf(GenericResponse.ErrorResponse::class.java, responseService)
        Assert.isTrue(CASHIN_PARTNER_MULTIPLE_PAYMENT_ERROR==responseService.objP, "message does not match")
    }
}