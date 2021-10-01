package com.mps.payment.core.client.sms

interface SmsClient {
    fun sendSmsMessage(request : SendMessageRequestInput): Boolean
}

data class SendMessageRequestInput(
    val number: String,
    val message: String
) {
    companion object {

        const val NUMBER_KEY = "toNumber"
        const val MESSAGE_KEY = "sms"
        const val DATE_KEY = "sendDate"

        const val FLASH_KEY = "flash"
        const val FLASH_VALUE = "0"

        const val SENDER_CODE_KEY = "sc"
        const val SENDER_CODE_VALUE = "890202"

        const val REQUEST_DELIVERY_RECEIPT_KEY = "request_dlvr_rcpt"
        const val REQUEST_DELIVERY_RECEIPT_VALUE = "0"
    }
}