package com.mps.payment.core.client.sms

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class SmsClientImpl(
        private val smsRequestBuilder: SmsRequestBuilder
) : SmsClient {

    @Value("\${sms.api.url}")
    lateinit var smsApiURL: String

    @Value("\${sms.api.account}")
    lateinit var smsApìAccount: String

    @Value("\${sms.api.key}")
    lateinit var smsApiKey: String

    @Value("\${sms.api.token}")
    lateinit var smsApiToken: String

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    var restTemplate = RestTemplate()

    override fun sendSmsMessage(input: SendMessageRequestInput): Boolean {
        val request = smsRequestBuilder.createSendSmsMessageRequestBody(input)
        return sendSMS(request)
    }

    fun sendSMS(data: Map<String, String>): Boolean {
        return try {
            val headers = HttpHeaders()
            headers.set("account", smsApìAccount)
            headers.set("apiKey", smsApiKey)
            headers.set("token", smsApiToken)
            val httpPayload = HttpEntity(data, headers)
            httpPayload.headers
            val response = restTemplate.exchange(smsApiURL, HttpMethod.POST, httpPayload, SmsResponse::class.java)
            println("hablame response: $response")
            return if (response.statusCode == HttpStatus.OK) {
                val responseBody: SmsResponse? = response.body
                if (responseBody != null) {
                    log.info("sms provider response $responseBody")
                }
                true
            } else {
                log.error("error calling sms provider", response)
                false

            }
        } catch (e: Exception) {
            log.error("Exception:error calling sms provider", e)
            false
        }
    }

}