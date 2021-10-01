package com.mps.payment.worker.task

import com.mps.common.dto.PaymentStateInput
import com.mps.payment.core.email.EmailSender
import com.mps.payment.core.enum.PaymentStateEnum
import com.mps.payment.core.model.Payment
import com.mps.payment.core.repository.BankingInformationRepository
import com.mps.payment.core.service.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


const val CLOSED_PAYMENT_TEMPLATE = "Closed_Report_Email"
const val SUBJECT_ABOUT_TO_CLOSE = "Tu pago esta a punto de salir de custodia"


@Component
class PaymentsToCloseTask(private val paymentService: PaymentService,
                          private val emailSender: EmailSender, private val merchantService: MerchantService,
                          private val customerService: CustomerService,
                          private val bankingInformationRepository: BankingInformationRepository) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val CLOSED_PAYMENTS = "closedPayments"

    @Value("\${fe.url}")
    lateinit var  url: String

    @Scheduled(fixedRate = 36000000)
    fun run() {
        try {
            log.info("executing close task")
            notifyClientsPaymentWillClose()
            val limitDate = LocalDate.now()
            val paymentsToClose = paymentService.getPaymentsByCloseDateAndState(limitDate,
                    listOf(PaymentStateEnum.SHIPPED.state,PaymentStateEnum.PAID.state))
            val transactionToDeleteAgreed = paymentService.getPaymentsByState(PaymentStateEnum.AGREED.state).filter { pay -> pay.creationDate!!
                    .plusDays(MAX_PERIOD.toLong()).isBefore(LocalDateTime.now()) } as MutableList
            val transactionToDeleteCreated = paymentService.getPaymentsByState(PaymentStateEnum.INITIAL.state).filter { pay -> pay.creationDate!!
                    .plusDays(MAX_PERIOD.toLong()).isBefore(LocalDateTime.now()) }
            val paymentsNotifiedAsReceived = paymentService.getPaymentsByState(PaymentStateEnum.RECEIVED.state)
            transactionToDeleteAgreed.addAll(transactionToDeleteCreated)
            deleteNotCompletedTransactions(transactionToDeleteAgreed)

            paymentsToClose.groupBy { it.idMerchant }.forEach {
                log.info("there are payments shipped to close ${it.value.size} for merchant ${it.key}")
                processPayment(it)
            }

            paymentsNotifiedAsReceived.groupBy { it.idMerchant }.forEach {
                log.info("there are payments shipped to close ${it.value.size} for merchant ${it.key}")
                processPayment(it)
            }
        } catch (e: Exception) {
            log.error("Error executing payment task to close",e)
        }
    }

    private fun deleteNotCompletedTransactions(payments: List<Payment>) {
        log.info("Deleting transactions "+payments.size)
        paymentService.deletePayment(payments)
    }

    private fun processPayment(listPaymentsPerMerchant: Map.Entry<UUID, List<Payment>>) {
        val merchant = merchantService.getMerchant(listPaymentsPerMerchant.key).get()
        val closedPaymentsPerMerchant = listPaymentsPerMerchant.value
        val accountInfo = bankingInformationRepository.findByMerchantId(merchant.id!!)
        val accountNumber = if(accountInfo.isEmpty){
            ""
        }else{
            accountInfo.get().accountNumber.toString()
        }
        emailSender.sendEmailWithTemplate(receiver = merchant.email, templateName = CLOSED_PAYMENT_TEMPLATE, title = SUBJECT_PAYMENT_WILL_CLOSE,
                o = getParamsForClosedEmail(partnerAccountNumber = accountNumber, closedPayments = closedPaymentsPerMerchant))
        updatePaymentToClose(closedPaymentsPerMerchant)
    }

    private fun notifyClientsPaymentWillClose(){
        log.info("Notifying payments about to close task")
        val nearDate = LocalDate.now().plusDays(1)
        val paymentsAboutToClose = paymentService.getPaymentsByCloseDateAndState(nearDate,
                listOf(PaymentStateEnum.SHIPPED.state,PaymentStateEnum.PAID.state))

        paymentsAboutToClose.forEach {
            val paymentUrl = "$url$PUBLIC_DETAIL/${it.id.toString().takeLast(6)}"
            val customer = customerService.getCustomerById(it.idCustomer!!)
            val merchant = merchantService.getMerchant(it.idMerchant)
            emailSender.sendEmailWithTemplate(receiver = customer.get().email, templateName = TEMPLATE_EMAIL_PLANE, title = SUBJECT_ABOUT_TO_CLOSE,
                    o = mapOf(CONST_MESSAGE to "Tienes un pago en custodia por el valor de ${it.amount} de una compra en ${merchant.get().name}. " +
                            "Si no reportas ninguna novedad, el día de mañana prrocedemos a pagar al vendedor. Para notificar novedades haz clic en el siguiente botón:",
                            LINK to paymentUrl,"headBody" to SUBJECT_ABOUT_TO_CLOSE))
        }
    }

    private fun updatePaymentToClose(listOfPaymentsToClose: List<Payment>) {
        listOfPaymentsToClose.forEach {
            log.info("updating to close payment ${it.id}")
            val paymentInput = PaymentStateInput(paymentId = it.id.toString(), state = PaymentStateEnum.CLOSED.state)
            paymentService.updateStateOfPayment(paymentInput, true)
        }
    }

    fun getParamsForClosedEmail(partnerAccountNumber: String, closedPayments: List<Payment>) = mapOf(CONST_MESSAGE to "Los siguientes pagos será cerrados. Si deseas el dinero en tu cuenta número $partnerAccountNumber " +
            "debes solicitar el retiro mediante el modulo de retiros de la plataforma.",
            CLOSED_PAYMENTS to closedPayments,
            CONST_TITLE_BODY to "Algunos pagos han cerrado su ciclo y estarán disponibles para que solicites el retiro de los fondos.")
}