package com.mps.payment.core.client.sms

data class SmsResponse(
        val status: String?,
        val account: String?,
        val smsId: String?,
        val execution_time: String?,
        val ip: String?
)