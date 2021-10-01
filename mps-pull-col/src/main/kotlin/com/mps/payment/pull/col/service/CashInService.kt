package com.mps.payment.pull.col.service

import com.mps.payment.pull.col.client.PaymentCoreClient
import com.mps.payment.pull.col.model.PaymentPartner
import com.mps.payment.pull.col.model.RedirectInformation
import com.mps.payment.pull.col.repository.PaymentPartnerRepository
import com.mps.payment.pull.col.util.generateSignature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


const val config = "SINGLE"
const val action = "PAYMENT"
const val currency = "170"
const val version = "V2"
const val mode = "INTERACTIVE"

const val CASH_IN_PARTNER_TRANS_STATUS_FIELD= "vads_trans_status"
const val CASH_IN_PARTNER_TRANS_ID_FIELD="vads_trans_id"
const val CASH_IN_PARTNER_PAYMENT_DONE_STATUS="CAPTURED"
const val CASH_IN_URL_RETURN="https://app.mipagoseguro.co/thanks-page"
const val CASH_IN_RETURN_MODE="GET"

@Service
class CashInService(private val paymentCoreClient: PaymentCoreClient,
                    private val paymentPartnerRepository: PaymentPartnerRepository) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Value("\${site.id}")
    private var siteId = "54188383"

    @Value("\${payzen.env}")
    private var environment = "TEST"

    @Value("\${payzen.key}")
    private var key = "AJkRToByTcchDHx6"

    fun createCashInRedirect(id: String): RedirectInformation? {
        val paymentDTO = paymentCoreClient.getPayment(id)
        if (paymentDTO == null) {
            log.error("payment was not recovered cause by token issue")
            return null
        }
        val actualDate = LocalDateTime.now(ZoneOffset.UTC)
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val finalDate = actualDate.format(formatter)
        val finalAmount = "${paymentDTO.amount.toString()}00"
        val finalId = paymentDTO.id.toString().takeLast(6)
        val paymentPartner = PaymentPartner(finalId, finalAmount, finalDate)
        paymentPartnerRepository.save(paymentPartner)
        return RedirectInformation(finalId,
                "", finalDate, version, siteId, mode, finalAmount, environment, currency, config, action, "", finalId)
    }

    fun setPaymentMethodAndGenerateSignature(paymentMethod: String, id: String): RedirectInformation? {
        val paymentPartnerOptional = paymentPartnerRepository.findById(id)
        if (paymentPartnerOptional.isEmpty) {
            log.error("redirect does not exist $id")
            return null
        }
        val paymentPartner = paymentPartnerOptional.get()
        val signature = generateSignature(
                buildMessageToEncrypt(paymentPartner.amount, paymentPartner.id, paymentPartner.transactionDate, paymentMethod), key)
        if (signature == null) {
            log.error("not enough information for generating signature")
            return null
        } else {
            log.info("updating redirect in database $id")
            paymentPartner.signature = signature
            paymentPartner.paymentMethod = paymentMethod
            try {
                paymentPartnerRepository.save(paymentPartner)
            }catch(e:Exception){
                log.error("error updating",e)
            }
        }
        return RedirectInformation(paymentPartner.id,
                signature, paymentPartner.transactionDate, version, siteId, mode, paymentPartner.amount, environment,
                currency, config, action, paymentMethod, paymentPartner.id)
    }

    fun processNotification(params: Map<String, String>):Boolean {
        val listKeyPairs: List<KeyPair> = params.map { KeyPair(it.key, it.value) }
        if(validateSignature(listKeyPairs)){
            val status = listKeyPairs.find { it.key== CASH_IN_PARTNER_TRANS_STATUS_FIELD}?.value ?: return false
            val paymentId = listKeyPairs.find { it.key== CASH_IN_PARTNER_TRANS_ID_FIELD }?.value ?: return false
            paymentPartnerRepository.updateFinalResultOfPayment(paymentId,status)
            return if(status==CASH_IN_PARTNER_PAYMENT_DONE_STATUS){
                paymentCoreClient.updatePaymentState(paymentId,3)
            }else{
                true
            }
        }else{
         return false
        }
    }

    private fun validateSignature(listKeyPairs: List<KeyPair>): Boolean {
        Collections.sort(listKeyPairs) { obj1, obj2 -> obj1.key.compareTo(obj2.key) }
        val signature = listKeyPairs.find { it.key == "signature" }?.value
        val message = buildMessageFromList(listKeyPairs)
        val calculatedSignature = generateSignature(message, key)
        return calculatedSignature == signature
    }

    private fun buildMessageFromList(listParams: List<KeyPair>): String {
        var message = ""
        listParams.forEach {
            if ("signature" != it.key) {
                message = if (message.isEmpty()) {
                    "${it.value}"
                } else {
                    "$message+${it.value}"
                }
            }
        }
        message = "${message}+${key}"
        return message
    }

    private fun buildMessageToEncrypt(amount: String, paymentId: String, actualDate: String, paymentMethod: String): String {
        val message = "${mode}+${amount}+${environment}+${currency}+${paymentId}+${action}+$paymentMethod+${config}+0+${CASH_IN_RETURN_MODE}+${siteId}+${actualDate}+${paymentId}+${CASH_IN_URL_RETURN}+${version}+${key}"
        log.info(message)
        return message
    }
}

data class KeyPair(val key: String, val value: String)