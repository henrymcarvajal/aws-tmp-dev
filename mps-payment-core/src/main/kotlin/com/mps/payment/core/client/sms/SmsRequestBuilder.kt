package com.mps.payment.core.client.sms

import org.springframework.stereotype.Service

@Service
class SmsRequestBuilder {

    fun createSendSmsMessageRequestBody(data: SendMessageRequestInput): Map<String, String> {
        val body = mutableMapOf<String, String>()
        body[SendMessageRequestInput.FLASH_KEY] = SendMessageRequestInput.FLASH_VALUE
        body[SendMessageRequestInput.SENDER_CODE_KEY] = SendMessageRequestInput.SENDER_CODE_VALUE
        body[SendMessageRequestInput.REQUEST_DELIVERY_RECEIPT_KEY] = SendMessageRequestInput.REQUEST_DELIVERY_RECEIPT_VALUE
        body[SendMessageRequestInput.NUMBER_KEY] = data.number.toString()
        body[SendMessageRequestInput.MESSAGE_KEY] = data.message.toString()
        body[SendMessageRequestInput.DATE_KEY] = System.currentTimeMillis().toString()
        return body
    }
}