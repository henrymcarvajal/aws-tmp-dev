package com.mps.payment.core.task

import com.mps.payment.core.email.EmailSenderImpl
import com.mps.payment.core.enum.PaymentStateEnum
import com.mps.payment.core.model.toEntity
import com.mps.payment.core.repository.BankingInformationRepository
import com.mps.payment.core.service.CustomerService
import com.mps.payment.core.service.MerchantService
import com.mps.payment.core.service.PaymentService
import com.mps.payment.core.service.TEMPLATE_EMAIL_PLANE
import com.mps.payment.core.util.createBankingInformation
import com.mps.payment.core.util.createMerchantTest
import com.mps.payment.core.util.createPaymentTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.time.LocalDate
import java.util.*

internal class PaymentsToCloseTaskTest {

    @Mock
    lateinit var emailSender: EmailSenderImpl

    @Mock
    lateinit var paymentService: PaymentService

    @Mock
    lateinit var merchantService: MerchantService

    @Mock
    lateinit var customerService: CustomerService

    @Mock
    lateinit var bankingInformationRepository: BankingInformationRepository


    lateinit var paymentTask: PaymentsToCloseTask

    //TODO make different scenarios

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        paymentTask = PaymentsToCloseTask(paymentService,
                emailSender, merchantService,customerService,bankingInformationRepository)
    }

    @Test
    @Ignore
    fun `testing closed transactions successful path`() {
        val payment = createPaymentTest()
        val merchant = createMerchantTest()
        val bankingInformation = createBankingInformation()
        val paymentEntity = payment.toEntity()
        val limitDate = LocalDate.now().minusDays(MAX_PERIOD.toLong())
        Mockito.`when`(paymentService.getPaymentsByDateAndState(date = limitDate, state = listOf(PaymentStateEnum.SHIPPED.state)))
                .thenReturn(listOf(paymentEntity))
        Mockito.`when`(merchantService.getMerchant(payment.idMerchant!!))
                .thenReturn(Optional.of(merchant.toEntity()))

        paymentTask.run()

        Mockito.verify(emailSender, Mockito.times(1)).sendEmailWithTemplate(receiver = merchant.email,
                templateName = CLOSED_PAYMENT_TEMPLATE, title = SUBJECT_PAYMENT_WILL_CLOSE,
                o = paymentTask.getParamsForClosedEmail(bankingInformation.accountNumber.toString(), listOf(paymentEntity)))
    }

    @Test
    @Ignore
    fun `testing when there is not closed transactions`() {
        val limitDate = LocalDate.now().minusDays(MAX_PERIOD.toLong())
        val payment = createPaymentTest()
        val merchant = createMerchantTest()
        val bankingInformation = createBankingInformation()
        Mockito.`when`(paymentService.getPaymentsByDateAndState(date = limitDate, state = listOf(PaymentStateEnum.SHIPPED.state)))
                .thenReturn(emptyList())
        Mockito.`when`(merchantService.getMerchant(payment.idMerchant!!))
                .thenReturn(Optional.of(merchant.toEntity()))

        paymentTask.run()
        Mockito.verify(emailSender, Mockito.times(0)).sendEmailWithTemplate(receiver = merchant.email,
                templateName = TEMPLATE_EMAIL_PLANE, title = SUBJECT_PAYMENT_WILL_CLOSE,
                o = paymentTask.getParamsForClosedEmail(bankingInformation.accountNumber.toString(), listOf(payment.toEntity())))
    }
}