package com.mps.payment.core.client.partner

import com.fasterxml.jackson.databind.ObjectMapper
import com.mps.common.auth.request.IntegrationInformation
import com.mps.common.auth.request.IntegrationRequest
import com.mps.payment.core.client.adapter.ObjectToUrlEncodedConverter
import com.mps.payment.core.util.client.createHeaders
import com.mps.payment.core.util.client.createTokenHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Service
class SecurityClient{

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    var restTemplate = RestTemplate()
    val MAXIMUM_ATTEMPTS_CALL = 3


    @Value("\${security.clientid}")
    lateinit var clientId:String

    @Value("\${security.secret}")
    lateinit var secret:String

    @Value("\${security.url}")
    lateinit var  url:String

    private var token: String? = null

    private fun createToken(){
        val headers = createHeaders(clientId, secret)
        headers.contentType= MediaType.APPLICATION_FORM_URLENCODED
        restTemplate.messageConverters.add(ObjectToUrlEncodedConverter(ObjectMapper()));
        val httpEntity = HttpEntity(mapOf("grant_type" to "client_credentials"), headers)
        val tokenResponse =
                restTemplate.exchange("${url}oauth/token", HttpMethod.POST, httpEntity, Map::class.java)
        token = tokenResponse.body!!["access_token"] as String
    }

    fun createIntegrationInfo(request: IntegrationRequest, attemptNumber: Int = 1): IntegrationInformation? {
        return try {
            if (token == null) {
                createToken()
            }
            val httpEntity = HttpEntity(request, createTokenHeader(token!!))
            val response =
                    restTemplate.exchange("${url}integration", HttpMethod.POST,
                            httpEntity, IntegrationInformation::class.java)
            if (HttpStatus.OK == response.statusCode) {
                log.info("integration info created response ${response.statusCode} body ${response.body}")
                response.body!!
            } else {
                log.warn("failed creating integration info ${response.statusCode} body ${response.body}")
                null
            }
        } catch (e: Exception) {
            if (e is HttpClientErrorException.Unauthorized && attemptNumber < MAXIMUM_ATTEMPTS_CALL) {
                log.error("Unauthorized attempt number $attemptNumber")
                createToken()
                createIntegrationInfo(request, attemptNumber + 1)
            } else {
                log.error("error creating integration info ${e.message} cause ${e.cause}")
                null
            }
        }
    }
}