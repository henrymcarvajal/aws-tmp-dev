package com.mps.payment.worker.task

import com.mps.payment.core.email.EmailSender
import com.mps.payment.core.enum.PaymentStateEnum
import com.mps.payment.core.model.Payment
import com.mps.payment.core.service.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate


const val MAX_PERIOD = 8
const val SUBJECT_PAYMENT_DONE = "Ya el comprador realizo el pago"
const val SUBJECT_PAYMENT_WILL_CLOSE = "Pagos cerrados"
const val ACCEPTED = "Aceptada"
const val MAX_PERIOD_EMAIL = 2

@Component
class PaymentTask(private val paymentService: PaymentService,
                  private val emailSender: EmailSender, private val customerService: CustomerService,
                  private val merchantService: MerchantService) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Value("\${fe.url}")
    lateinit var url: String

    /**
     * 1. verify agreed transactions are now paid or not and change to new state, send email to seller
     * 2. get time of shipped payment and verify if it was 6 days ago, if payment is not disputed change state to closed, transfer money to seller, send email to seller
     */
    @Scheduled(fixedRate = 18000000)
    fun run() {
        try {
            log.info("executing payment task")
            val agreedButNotPaidPayments = paymentService.getPaymentsByState(PaymentStateEnum.AGREED.state)
            val limitDate = LocalDate.now().minusDays(MAX_PERIOD_EMAIL.toLong()).atStartOfDay()
            agreedButNotPaidPayments.filter { it.modificationDate.isAfter(limitDate) || it.modificationDate.isEqual(limitDate) }
                    .forEach {
                        sendPromotionEmail(it)
                    }
        } catch (e: Exception) {
            log.error("Error executing payment task",e)
        }
    }

    private fun sendPromotionEmail(payment: Payment) {
        if (payment.linkUrl != null) {
            log.info("enviando correo para concretar compra ${payment.id}")
            val customer = customerService.getCustomerById(payment.idCustomer!!).get()
            val merchant = merchantService.getMerchant(payment.idMerchant).get()
            val message = "Tienes un pago pendiente por el valor de ${payment.amount} en la tienda ${merchant.name}. Nuestro servicio de contraentrega digital recibira tu pago. Pero el vendedor solo recibirá el dinero cuando te llegue el pedido. Si tu pago es por Efecty  y ya tienes pines de pago, simplemente acercate a la sucursal más cercana para pagar :) . De lo contrario puedes continuar tu pago con el siguiente botón."
            emailSender.sendEmailWithTemplate(receiver = customer.email, templateName = CLOSE_BUY_TEMPLATE, title = "Compra tranquilo con MiPagoSeguro",
                    o = mapOf(CONST_MESSAGE to message, LINK to payment.linkUrl))
        }
    }
}