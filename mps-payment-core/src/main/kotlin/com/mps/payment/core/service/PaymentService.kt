package com.mps.payment.core.service

import com.mps.common.dto.*
import com.mps.payment.core.email.EmailSender
import com.mps.payment.core.enum.PaymentStateEnum
import com.mps.payment.core.model.*
import com.mps.payment.core.repository.PaymentRepository
import com.mps.payment.core.task.MAX_PERIOD
import com.mps.payment.core.task.SUBJECT_PAYMENT_DONE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

const val CASHIN_PARTNER_MULTIPLE_PAYMENT_ERROR="more than one pay for partner id"
const val EXISTING_MESSAGE_ERROR = "id Payment already exists"
const val MERCHANT_NOT_EXIST = "Merchant does not exist"
const val PAYMENT_NOT_EXIST = "payment does not exist"
const val PAYMENT_INCORRECT_STATE = "state of payment is incorrect for agree stage"
const val STATE_NOT_VALID = "state is not valid"
const val DELIVERY_INFO_MISSING = "Delivery information is missing"
const val PAYMENT_NOT_HAVE_CUSTOMER = "payment does not have customer"
const val UNAUTHORIZED_UPDATE = "customer trying to update is not the owner of payment"
const val TEMPLATE_EMAIL_SELLER = "pago_generado_vendedor"
const val TEMPLATE_EMAIL_PLANE = "plane_template_buyer"
const val TEMPLATE_ORDER_CONFIRMED = "order_confirmed_buyer"
const val TEMPLATE_EMAIL_PLANE_GENERIC = "plane_template_generic"
const val CLOSE_BUY_TEMPLATE= "come_to_pay_buyer"
const val SUBJECT_SELLER = "Nuevo Pago generado"
const val SUBJECT_BUYER = "Tu pago estará en custodia"
const val ORDER_CONFIRMED = "Tu orden ha sido recibida"
const val URL_BUYER = "agree-payment"
const val PUBLIC_DETAIL = "detail"
const val CONSTANT_COST_RESULT = "resultadoCosto"
const val LINK = "link"
const val CONST_MESSAGE = "message"
const val CONST_TITLE_BODY = "title_body"
const val DEFAULT_COMMISION = 3.5
const val PROMOTION_FEE= 2.8
const val TITLE_EMAIL_TEMPLATE_BUYER_AGREE = "Pago generado! Ahora puedes realizar el pago y estar tranquilo."
const val MESSAGE_EMAIL_BUYER_AGREE_1 = "Has aceptado un pago seguro por el valor de "
const val MESSAGE_EMAIL_BUYER_AGREE_2 = "Una vez el pago sea efectuado y nuestro sistema lo verifique, el vendedor procederá con el envio de tu producto."

@Service
class PaymentService(private val paymentRepository: PaymentRepository, private val merchantService: MerchantService,
                     private val customerService: CustomerService,
                     private val emailSender: EmailSender
                    ) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Value("\${fe.url}")
    lateinit var  url: String

    fun processWooPayment(wooPayment: WooPayment):GenericResponse<*>{
        val paymentCreated = createPayment(wooPayment.payment)
        val paymentAgree = PaymentAgree(idPayment = (paymentCreated.objP as PaymentDTO).publicId, customer = wooPayment.customer)
        return agreePayment(paymentAgree)
    }

    fun createPayment(paymentDTO: PaymentDTO,callFromProduct:Boolean=false): GenericResponse<*> {
        paymentDTO.id?.let {
            if (paymentRepository.existsById(it)) {
                return GenericResponse.ErrorResponse(EXISTING_MESSAGE_ERROR)
            }
        }
        val merchant = merchantService.getMerchant(paymentDTO.idMerchant!!)
        if (merchant.isEmpty) {
            return GenericResponse.ErrorResponse(MERCHANT_NOT_EXIST)
        }
        val payment = paymentDTO.toEntity()
        payment.creationDate = LocalDateTime.now()
        payment.comision= calculateFee(merchant.get(),payment.amount)

        val newPaymentDTO = paymentRepository.save(payment).toDTO()
        if (!callFromProduct) {
            val shortId = newPaymentDTO.id.toString().takeLast(6)
            val paymentUrl = "$url$URL_BUYER/${shortId}"
            emailSender.sendEmailWithTemplate(templateName = TEMPLATE_EMAIL_SELLER, title = SUBJECT_SELLER,
                    o = mapOf(LINK to paymentUrl, CONSTANT_COST_RESULT to paymentDTO.amount),
                    receiver = merchant.get().email)
        }
        return GenericResponse.SuccessResponse(newPaymentDTO)
    }

    private fun calculateFee(merchant: Merchant, amount: BigDecimal): BigDecimal {
        val commission = calculateCommission(merchant)
        return (amount.multiply(commission)).divide(BigDecimal.valueOf(100)) + BigDecimal.valueOf(900)
    }

    fun save(payment:Payment) = paymentRepository.save(payment)


    private fun calculateCommission(merchant: Merchant): BigDecimal {
        val actualDate = LocalDateTime.now()
        return if (actualDate > merchant.creationDate!!.plusDays(28)) {
            BigDecimal(DEFAULT_COMMISION)
        } else {
            BigDecimal(PROMOTION_FEE)
        }
    }

    fun agreePayment(paymentAgree: PaymentAgree): GenericResponse<*> {
        val paymentList = getPaymentByShortId(paymentAgree.idPayment!!)
        if (paymentList.isEmpty()) {
            return GenericResponse.ErrorResponse(PAYMENT_NOT_EXIST)
        }

        val payment = paymentList[0]
        if (PaymentStateEnum.AGREED.state == payment.idState) {
            return GenericResponse.SuccessResponse(payment.id.toString().takeLast(6))
        }

        if (PaymentStateEnum.INITIAL.state != payment.idState) {
            return GenericResponse.ErrorResponse(PAYMENT_INCORRECT_STATE)
        }
        val newCustomer = customerService.createOrReplaceCustomer(paymentAgree.customer)
        payment.idCustomer = newCustomer.id
        payment.idState = PaymentStateEnum.AGREED.state
        payment.closeDate = LocalDateTime.now().plusDays(MAX_PERIOD.toLong())
        paymentRepository.save(payment)
        val paymentUrl = "$url$PUBLIC_DETAIL/${payment.id.toString().takeLast(6)}"
        emailSender.sendEmailWithTemplate(receiver = newCustomer.email, templateName = TEMPLATE_EMAIL_PLANE, title = SUBJECT_BUYER,
                o = mapOf(CONST_MESSAGE to "$MESSAGE_EMAIL_BUYER_AGREE_1${payment.amount}.$MESSAGE_EMAIL_BUYER_AGREE_2. " +
                        "Si deseas hacer seguimiento de tu pedido o notificar novedades usa el siguiente botón:",
                        LINK to paymentUrl, "headBody" to SUBJECT_BUYER))
        return GenericResponse.SuccessResponse(payment.id.toString().takeLast(6))
    }

    fun getPayment(id: UUID) = paymentRepository.findById(id)
    fun getPaymentByShortId(id:String): List<Payment> {
        return if(id==null || id.isBlank()){
            listOf()
        }else{
            paymentRepository.getPaymentByShortId(id)
        }
    }

    fun getPaymentsByState(state: Int) = paymentRepository.getAllPaymentsByState(state)

    fun getPaymentsPerMerchant(merchantId: UUID, queryParams: QueryParams?): List<PaymentWithCustomerDTO> {

        val payments = if(queryParams!=null){
            var limitDate:LocalDateTime? = null

            if(queryParams.duration>0){
                val actualDate = LocalDateTime.now()
                limitDate = actualDate.minusDays(queryParams.duration.toLong())
            }
            var paymentState:Int? = null
            if(queryParams.state>0){
                paymentState= queryParams.state
            }

            paymentRepository.findPaymentsPerMerchantDynamicParams(merchantId,limitDate,paymentState)
        }else{
            paymentRepository.findPaymentByIdMerchantByDesc(merchantId)
        }

        return payments.map {
            var customerName = ""
            if (it.idCustomer != null) {
                val customer = customerService.getCustomerById(it.idCustomer!!)
                if (customer.isPresent) {
                    customerName = customer.get().name
                }
            }
            val dto = it.toWithCustomerDTO()
            dto.customerName = customerName
            dto
        }
    }

    fun getPaymentsByDateAndState(date: LocalDate, state: List<Int>) =
            paymentRepository.getPaymentsByDateAndState(date, state)

    fun getPaymentsByCloseDateAndState(date: LocalDate, state: List<Int>) =
            paymentRepository.getPaymentsByCloseDateAndState(date, state)

    fun delayCloseDate(delayCloseDateToPaymentInput: DelayCloseDateToPaymentInput): GenericResponse<*> {
        val id = delayCloseDateToPaymentInput.id
        val payment = getPayment(id)

        return if (payment.isEmpty) {
            log.error("trying to delay a payment that does not exist ID $id")
            GenericResponse.ErrorResponse(PAYMENT_NOT_EXIST)
        } else {
            val customer = customerService.getCustomerById(payment.get().idCustomer!!)
            if(customer.isEmpty){
                log.error("Customer does not exist")
                return GenericResponse.ErrorResponse("Customer does not exist")
            }
            if (customer.get().numberId != delayCloseDateToPaymentInput.identificationCustomer) {
                log.error("Identification does not match")
                return GenericResponse.ErrorResponse("Identification does not match")
            }
            val paymentValue = payment.get()
            paymentValue.closeDate = paymentValue.closeDate!!.plusDays(3)
            GenericResponse.SuccessResponse(paymentRepository.save(paymentValue).toDTO())
        }
    }

    fun updateStateOfPayment(paymentStateInput: PaymentStateInput, isInternalCall: Boolean=false): GenericResponse<*> {
        log.info("updating state of payment input $paymentStateInput")
        var existByPartner = false
        if(paymentStateInput.paymentId.length==6){
            val payment= getPaymentByShortId(paymentStateInput.paymentId)
            if(payment.isEmpty()){
                log.error("CASHIN case: payment does not existt ${paymentStateInput.paymentId}")
                return GenericResponse.ErrorResponse(PAYMENT_NOT_EXIST)
            }
            if(payment.size>1){
                log.error("$CASHIN_PARTNER_MULTIPLE_PAYMENT_ERROR ${paymentStateInput.paymentId}")
                return GenericResponse.ErrorResponse(CASHIN_PARTNER_MULTIPLE_PAYMENT_ERROR)
            }
            paymentStateInput.paymentId=payment[0].id.toString()
            existByPartner= true
        }
        val paymentId= UUID.fromString(paymentStateInput.paymentId)
        if (!existByPartner && !paymentRepository.existsById(paymentId)) {
            log.error("payment does not existt ${paymentStateInput.paymentId}")
            return GenericResponse.ErrorResponse(PAYMENT_NOT_EXIST)
        }
        if (PaymentStateEnum.values().find { it.state == paymentStateInput.state } == null) {
            log.error("invalid state input ${paymentStateInput.state}")
            return GenericResponse.ErrorResponse(STATE_NOT_VALID)
        }
        if (PaymentStateEnum.SHIPPED.state != paymentStateInput.state &&
                PaymentStateEnum.DISPUTED.state != paymentStateInput.state && !isInternalCall && !existByPartner) {
            log.error("no authorized update")
            return GenericResponse.ErrorResponse(STATE_NOT_VALID)
        }
        if (PaymentStateEnum.SHIPPED.state == paymentStateInput.state  && (paymentStateInput.guideNumber == null || paymentStateInput.transportCompany == null)){
            log.error(DELIVERY_INFO_MISSING)
            return GenericResponse.ErrorResponse(DELIVERY_INFO_MISSING)
        }
        if(PaymentStateEnum.PAID.state == paymentStateInput.state){
            val existingPayment = paymentRepository.findById(paymentId).get()
            val merchant = merchantService.getMerchant(existingPayment.idMerchant).get()
            val customer = customerService.getCustomerById(existingPayment.idCustomer!!).get()
            emailSender.sendEmailWithTemplate(receiver = merchant.email, templateName = TEMPLATE_EMAIL_PLANE_GENERIC, title = SUBJECT_PAYMENT_DONE,
                    o = getParamsForPaidEmail(customer = customer, amount = existingPayment.amount, id = existingPayment.id))
        }
        if(PaymentStateEnum.SHIPPED.state == paymentStateInput.state){
            paymentRepository.updateStateAndDeliveryInfoOfPayment(paymentId = paymentId, state = paymentStateInput.state,
            guideNumber = paymentStateInput.guideNumber!!, transportCompany = paymentStateInput.transportCompany!!)
        }else{
            paymentRepository.updateStateOfPayment(paymentId = paymentId, state = paymentStateInput.state)
        }
        val payment = paymentRepository.findById(paymentId).get()
        return GenericResponse.SuccessResponse(payment.toDTO())
    }

    fun updateStatePublicOfPayment(shortId: String, state: Int, numberId : String): GenericResponse<*> {

        log.info("updating state of payment public")
        val paymentOptional = getPaymentByShortId(shortId)
        if(paymentOptional.isEmpty()) {
            log.error("payment does not existt $shortId")
            return GenericResponse.ErrorResponse(PAYMENT_NOT_EXIST)
        }
        val payment = paymentOptional[0]
        if(PaymentStateEnum.PAID.state != payment.idState && PaymentStateEnum.SHIPPED.state != payment.idState){
            log.error("state not valid")
            return GenericResponse.ErrorResponse(STATE_NOT_VALID)
        }
        if (state != PaymentStateEnum.DISPUTED.state && PaymentStateEnum.RECEIVED.state != state) {
            log.error("Not authorized update")
            return GenericResponse.ErrorResponse(STATE_NOT_VALID)
        }
        if(payment.idCustomer ==null){
            return GenericResponse.ErrorResponse(PAYMENT_NOT_HAVE_CUSTOMER)
        }
        val customer = customerService.getCustomerById(payment.idCustomer!!)

        if(numberId != customer.get().numberId){
            return GenericResponse.ErrorResponse(UNAUTHORIZED_UPDATE)
        }
        val paymentId = payment.id
        if(PaymentStateEnum.DISPUTED.state == state){
            log.info("updating state to disputed sending emails")
            val existingPayment = paymentRepository.findById(paymentId).get()
            emailSender.sendEmailWithTemplate(templateName = TEMPLATE_GENERIC_BUTTON, title = "Nueva disputa sobre pago",
                    o = mapOf(LINK to "${url}$PUBLIC_DETAIL/${paymentId.toString().takeLast(6)}", CONST_MESSAGE to "El pago de valor ${existingPayment.amount} con id ${existingPayment.id}.", "titleMessage" to "Nueva disputa",
                            "buttonText" to "Ver pago"),
                    receiver = "mipagoseguro.col@gmail.com")
            val merchant = merchantService.getMerchant(existingPayment.idMerchant).get()
            val customer = customerService.getCustomerById(existingPayment.idCustomer!!).get()
            emailSender.sendEmailWithTemplate(templateName = TEMPLATE_GENERIC_BUTTON, title = "Nueva disputa sobre pago",
                    o = mapOf(LINK to "${url}transaction-detail/${paymentId}", CONST_MESSAGE to "el cliente ${customer.name} ${customer.lastName} ha creado una disputa sobre el pago de valor ${existingPayment.amount} con id ${existingPayment.id}. Ponte en contacto con tu cliente al número ${customer.contactNumber} para que des solución al caso. " +
                            "Recuerda que esta etapa de la disputa dura 6 días hábiles.", "titleMessage" to "Nueva disputa",
                            "buttonText" to "Ver pago"),
                    receiver = merchant.email)
        }

        paymentRepository.updateStateOfPayment(paymentId = paymentId, state = state)
        val paymentToReturn = paymentRepository.findById(paymentId).get()
        return GenericResponse.SuccessResponse(paymentToReturn.toDTO())
    }

    fun deletePayment(payments:List<Payment>) = paymentRepository.deleteInBatch(payments)

    fun getParamsForPaidEmail(customer: Customer, amount: BigDecimal, id: UUID): Map<String, String> {
        val lastName = customer.lastName?.let { it } ?: ""
        var message = "El comprador ${customer.name} $lastName ya realizó el pago por el valor de $amount. " +
                "Ya puedes proceder a realizar el despacho del pedido. Una vez despaches, debes cambiar el estado del pago mediante la pantalla de detalle del pago a Despachado."
        if (customer.address != null && !customer.address.isNullOrEmpty()) {
            message = "$message La dirección es ${customer.address}, barrio ${customer.neighborhood}, ciudad ${customer.city}, departamento ${customer.department}."
        }
        return mapOf(CONST_MESSAGE to message,
                CONST_TITLE_BODY to "Tu comprador ha realizado el pago", LINK to "${url}transaction-detail/$id")
    }
}
