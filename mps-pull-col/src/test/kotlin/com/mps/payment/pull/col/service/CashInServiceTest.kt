package com.mps.payment.pull.col.service

import com.mps.payment.pull.col.client.PaymentCoreClient
import com.mps.payment.pull.col.model.RedirectInformation
import com.mps.payment.pull.col.repository.PaymentPartnerRepository
import com.mps.payment.pull.col.util.createPaymentPartner
import com.mps.payment.pull.col.util.createPaymentTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import org.springframework.util.Assert
import java.util.Optional


internal class CashInServiceTest {

    private lateinit var cashInService:CashInService

    @Mock
    private lateinit var paymentPartnerRepository: PaymentPartnerRepository

    @Mock
    private lateinit var client: PaymentCoreClient

    @BeforeEach
    fun setup(){
        MockitoAnnotations.initMocks(this)
        cashInService = CashInService(client,paymentPartnerRepository)
    }

    @Test
    fun `process notification success`() {
        val paymentId = "123456"
        val params = mapOf("vads_effective_creation_date" to "101212","vads_presentation_date" to "121221",
                "vads_card_number" to "123456",CASH_IN_PARTNER_TRANS_STATUS_FIELD to CASH_IN_PARTNER_PAYMENT_DONE_STATUS,
                CASH_IN_PARTNER_TRANS_ID_FIELD to paymentId,"signature" to "MbGdQnTKCEF2PnKljn+G80JeyVqYSf6oA3b+uJlvWP8=")
        Mockito.`when`(client.updatePaymentState(paymentId,3)).thenReturn(true)
        val response = cashInService.processNotification(params)
        Mockito.verify(client, times(1)).updatePaymentState(paymentId,3)
        Assert.isTrue(response,"was not updated")
    }

    @Test
    fun `process notification trans id null`() {
        val params = mapOf("vads_effective_creation_date" to "101212","vads_presentation_date" to "121221",
                "vads_card_number" to "123456",CASH_IN_PARTNER_TRANS_STATUS_FIELD to CASH_IN_PARTNER_PAYMENT_DONE_STATUS,
                "signature" to "MbGdQnTKCEF2PnKljn+G80JeyVqYSf6oA3b+uJlvWP8=")
        val response = cashInService.processNotification(params)
        Assert.isTrue(!response,"It is true")
    }

    @Test
    fun `process notification trans status null`() {
        val paymentId = "123456"
        val params = mapOf("vads_effective_creation_date" to "101212","vads_presentation_date" to "121221",
                "vads_card_number" to "123456", CASH_IN_PARTNER_TRANS_ID_FIELD to paymentId,
                "signature" to "MbGdQnTKCEF2PnKljn+G80JeyVqYSf6oA3b+uJlvWP8=")
        val response = cashInService.processNotification(params)
        Assert.isTrue(!response,"It is true")
    }

    @Test
    fun `process notification invalid signature`() {
        val paymentId = "123456"
        val params = mapOf("vads_effective_creation_date" to "101212","vads_presentation_date" to "121221",
                "vads_card_number" to "123456",CASH_IN_PARTNER_TRANS_STATUS_FIELD to CASH_IN_PARTNER_PAYMENT_DONE_STATUS,
                CASH_IN_PARTNER_TRANS_ID_FIELD to paymentId,"signature" to "MbGdQnTKCEF2PnKljn+G80JeyVqYSf6oA3b+uJlvWP8=66")
        val response = cashInService.processNotification(params)
        Assert.isTrue(!response,"It is true")
    }

    @Test
    fun `set payment method redirect not exist`() {
        val redirectId = "123456"
        Mockito.`when`(paymentPartnerRepository.findById(redirectId)).thenReturn(Optional.empty())
        val response = cashInService.setPaymentMethodAndGenerateSignature("",redirectId)
        Assert.isNull(response,"It is not null")
    }
    @Test
    fun `set payment method happy path`() {
        val redirectId = "123456"
        Mockito.`when`(paymentPartnerRepository.findById(redirectId))
                .thenReturn(Optional.of(createPaymentPartner()))
        val response = cashInService.setPaymentMethodAndGenerateSignature("",redirectId)
        Assert.isInstanceOf(RedirectInformation::class.java, response)
    }

    @Test
    fun `create redirect payment does not exist`() {
        val paymentId = "12345"
        Mockito.`when`(client.getPayment(paymentId)).thenReturn(null)
        val response = cashInService.createCashInRedirect(paymentId)
        Assert.isNull(response,"this is not null")
    }

    @Test
    fun `create redirect payment happy path`() {
        val paymentId = "12345"
        Mockito.`when`(client.getPayment(paymentId)).thenReturn(createPaymentTest())
        val response = cashInService.createCashInRedirect(paymentId)
        Assert.isInstanceOf(RedirectInformation::class.java, response)
    }
}