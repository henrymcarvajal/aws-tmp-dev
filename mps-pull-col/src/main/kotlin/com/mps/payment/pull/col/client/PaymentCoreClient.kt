package com.mps.payment.pull.col.client

import com.mps.common.auth.request.AuthenticationRequest
import com.mps.common.dto.*
import com.mps.payment.core.security.createTextEncryption
import com.mps.payment.core.util.client.createTokenHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory




@Service
class PaymentCoreClient {

    @Value("\${backend.url}")
    private val url: String = "http://localhost:8083/mps/"

    private var restTemplate = RestTemplate()

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Value("\${user.admin}")
    private lateinit var userAdmin: String

    @Value("\${user.pass}")
    private lateinit var pass: String

    private var token = ""

    fun authenticate(): String? {
        val httpEntity = HttpEntity(AuthenticationRequest(userAdmin, pass))
        val tokenResponse =
                restTemplate.exchange("${url}auth/signin", HttpMethod.POST, httpEntity, Map::class.java)
        return if (tokenResponse.body != null) {
            tokenResponse.body!!["token"] as String
        } else {
            null
        }
    }

    fun processPayment(wooPayment: WooPayment, attempts: Int = 0): String? {
        return try {
            if(!configureCall(attempts)){
                log.error("error calling create customer")
                return null
            }
            val httpEntity = HttpEntity(wooPayment, createTokenHeader(token))
            val agreeResponse =
                    restTemplate.exchange("${url}payment/woo", HttpMethod.POST, httpEntity, String::class.java)
            if(agreeResponse.statusCode.isError){
                log.error("creation customer response as a error {}",agreeResponse)
                return null
            }
            agreeResponse.body

        } catch (e: HttpClientErrorException) {
            if (403 == e.rawStatusCode) {
                authenticate()?.let { token = it } ?: let {
                    log.error("catch authentication failed")
                    null
                }
                processPayment(wooPayment, attempts + 1)
            } else {
                null
            }
        } catch (e: Exception) {
            log.error("error calling core {}", e)
            null
        }
    }

    private fun configureCall(attempts: Int):Boolean{
        val requestFactory = HttpComponentsClientHttpRequestFactory()
        requestFactory.setConnectTimeout(10000)
        requestFactory.setReadTimeout(10000)
        restTemplate.requestFactory= requestFactory
        if (attempts == 3) {
            return false
        }
        if (token.isEmpty()) {
            authenticate()?.let { token = it } ?: let {
                log.error("authentication failed")
                return false
            }
        }
        return true
    }

    fun updatePaymentState(paymentId:String,state:Int, attempts: Int = 0): Boolean {
        val paymentStateInput = PaymentStateInput(paymentId,state)
        return try {
            if(!configureCall(attempts)){
                return false
            }
            val httpEntity = HttpEntity(paymentStateInput, createTokenHeader(token))
            val paymentResponse =
                    restTemplate.exchange("${url}payment/updateState/notification", HttpMethod.PATCH, httpEntity, PaymentDTO::class.java)
            if(paymentResponse.statusCode.isError){
                log.error("payment core respoonse is an error {}",paymentResponse)
                return false
            }
            true

        } catch (e: HttpClientErrorException) {
            if (403 == e.rawStatusCode) {
                authenticate()?.let { token = it } ?: let {
                    log.error("catch authentication failed")
                    false
                }
                updatePaymentState(paymentId,state, attempts + 1)
            } else {
                false
            }
        } catch (e: Exception) {
            log.error("error calling core", e)
            false
        }
    }

    fun requestFreight(paymentId: String, attempts: Int = 0): Boolean {
        return try {
            if (!configureCall(attempts)) {
                return false
            }
            val httpEntity = HttpEntity(paymentId, createTokenHeader(token))
            val response =
                    restTemplate.exchange("${url}logistic/freight", HttpMethod.POST, httpEntity, String::class.java)
            if (response.statusCode.isError) {
                log.error("payment core respoonse is an error {}", response)
                return false
            }
            true
        } catch (e: HttpClientErrorException) {
            if (403 == e.rawStatusCode) {
                authenticate()?.let { token = it } ?: let {
                    log.error("catch authentication failed")
                    false
                }
                requestFreight(paymentId,attempts + 1)
            } else {
                false
            }
        } catch (e: Exception) {
            log.error("error calling core", e)
            false
        }
    }

    fun getPayment(id: String, attempts: Int = 0): PaymentDTO? {
        return try {
            if (attempts == 3) {
                return null
            }
            if (token.isEmpty()) {
                authenticate()?.let { token = it } ?: let {
                    log.error("authentication failed")
                    return null
                }
            }
            val httpEntity = HttpEntity(emptyMap<String, String>(), createTokenHeader(token))
            val paymentResponse =
                    restTemplate.exchange("${url}payment/${id}", HttpMethod.GET, httpEntity, PaymentDTO::class.java)

            if(paymentResponse.statusCode.isError){
                log.error("payment core respoonse is an error {}",paymentResponse)
                return null
            }

            return paymentResponse.body
        } catch (e: HttpClientErrorException) {
            log.error("error hitting payment core",e)
            if (403 == e.rawStatusCode) {
                authenticate()?.let { token = it } ?: let {
                    log.error("catch authentication failed")
                    return null
                }
                getPayment(id, attempts + 1)
            } else {
                null
            }
        } catch (e: Exception) {
            log.error("error calling core", e)
            null
        }
    }
}