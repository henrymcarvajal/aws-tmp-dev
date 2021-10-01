package com.mps.payment.core.client.entregalo

import com.mps.payment.core.client.entregalo.payload.*
import com.mps.payment.core.client.entregalo.payload.queryservicestatus.external.ExternalQueryServiceStatusResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate


@Service
class EntregaloClientImpl : EntregaloClient {

    @Value("\${entregalo.api.url}")
    lateinit var apiURL: String

    @Value("\${entregalo.api.token}")
    lateinit var apiToken: String

    @Value("\${entregalo.api.retryCount}")
    lateinit var retryCount: String

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    var restTemplate = RestTemplate()

    override fun sendFreightRequest(data: AskNewServiceRequestInput): AskNewServiceResponse {

        var attempts = 0
        var mustRetry = false
        activatingLogs()
        var askNewServiceResponse = AskNewServiceResponse(HttpStatus.OK)

        do {
            log.info("attempt: $attempts")
            Thread.sleep((5 * 1000 * attempts).toLong())
            try {
                val headers = HttpHeaders()
                headers.set("token", apiToken)
                val httpPayload = HttpEntity(data, headers)
                val response =
                    restTemplate.exchange("$apiURL/v2/shippings/new", HttpMethod.POST, httpPayload, EntregaloResponse::class.java)
                if (response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.CREATED) {
                    val responseBody: EntregaloResponse? = response.body
                    if (responseBody != null) {
                        askNewServiceResponse = AskNewServiceResponse(response.statusCode, responseBody.data.Guia,responseBody.data.Etiqueta)
                    }
                } else {
                    askNewServiceResponse = AskNewServiceResponse(response.statusCode)
                }
            } catch (e: HttpClientErrorException.BadRequest) {
                log.error("error calling entregalo",e)
                askNewServiceResponse = AskNewServiceResponse(HttpStatus.BAD_REQUEST)
            } catch (e: HttpClientErrorException.Unauthorized) {
                log.error("error calling entregalo",e)
                askNewServiceResponse = AskNewServiceResponse(HttpStatus.UNAUTHORIZED)
            } catch (e: HttpClientErrorException.Forbidden) {
                log.error("error calling entregalo",e)
                askNewServiceResponse = AskNewServiceResponse(HttpStatus.FORBIDDEN)
            } catch (e: HttpClientErrorException.NotFound) {
                log.error("error calling entregalo",e)
                askNewServiceResponse = AskNewServiceResponse(HttpStatus.NOT_FOUND)
            } catch (e: Exception) {
                log.error("error calling entregalo, attempt number $attempts",e)
                mustRetry = true
                attempts++
                askNewServiceResponse = AskNewServiceResponse(HttpStatus.NOT_FOUND, " Exception: $e")
            }
        } while (mustRetry && attempts <= retryCount.toInt())

        return askNewServiceResponse
    }
    override fun sendQueryServiceStatusRequest(data: QueryStatusRequest): QueryServiceStatusResponse {

        var attempts = 0
        var mustRetry = false
        activatingLogs()
        var internalQueryServiceStatusResponse = QueryServiceStatusResponse(HttpStatus.OK)

        do {
            log.info("attempt: $attempts")
            Thread.sleep((5 * 1000 * attempts).toLong())
            try {
                val headers = HttpHeaders()
                headers.set("token", apiToken)
                val httpPayload = HttpEntity(data, headers)
                log.debug("httpPayload: $httpPayload")
                val response =
                        restTemplate.exchange("$apiURL/shippings/currentStatus", HttpMethod.POST, httpPayload, ExternalQueryServiceStatusResponse::class.java)
                if (response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.CREATED) {
                    val responseBody: ExternalQueryServiceStatusResponse? = response.body
                    if (responseBody != null) {
                        internalQueryServiceStatusResponse = QueryServiceStatusResponse(response.statusCode, responseBody.data.Status,
                                responseBody.data.Valor)
                    }
                } else {
                    internalQueryServiceStatusResponse = QueryServiceStatusResponse(response.statusCode)
                }
            } catch (e: HttpClientErrorException.BadRequest) {
                log.error("error consulting guide",e)
                internalQueryServiceStatusResponse = QueryServiceStatusResponse(HttpStatus.BAD_REQUEST)
            } catch (e: HttpClientErrorException.Unauthorized) {
                log.error("error consulting guide",e)
                internalQueryServiceStatusResponse = QueryServiceStatusResponse(HttpStatus.UNAUTHORIZED)
            } catch (e: HttpClientErrorException.Forbidden) {
                log.error("error consulting guide",e)
                internalQueryServiceStatusResponse = QueryServiceStatusResponse(HttpStatus.FORBIDDEN)
            } catch (e: HttpClientErrorException.NotFound) {
                log.error("error consulting guide",e)
                internalQueryServiceStatusResponse = QueryServiceStatusResponse(HttpStatus.NOT_FOUND)
            } catch (e: Exception) {
                log.error("error consulting guide, attempt $attempts $mustRetry",e)
                mustRetry = true
                attempts++
                internalQueryServiceStatusResponse = QueryServiceStatusResponse(HttpStatus.NOT_FOUND, " Exception: $e")
            }
        } while (mustRetry && attempts <= retryCount.toInt())

        return internalQueryServiceStatusResponse
    }

    override fun getCities(): List<CityDTO>? {
        var attempts = 0
        var mustRetry = false
        activatingLogs()
        do {
            log.info("attempt: $attempts")
            Thread.sleep((5 * 1000 * attempts).toLong())
            try {
                val headers = HttpHeaders()
                headers.set("token", apiToken)
                val httpPayload = HttpEntity({},headers)
                val response =
                        restTemplate.exchange("$apiURL/cities", HttpMethod.POST, httpPayload, Map::class.java)

                return ((response.body?.get("data") as Map<*,*>)["Ciudades"] as List<Map<*,*>>?)?.map { map->CityDTO(
                        code=map["code"].toString(),
                        city = map["city"].toString(),
                        state = map["state"].toString(),
                        cityExtended = map["cityExtended"].toString(),
                        againstDelivery = map["againstDelivery"].toString()
                ) }

            } catch (e: HttpClientErrorException.BadRequest) {
                log.error("error getting cities",e)
                return null
            } catch (e: HttpClientErrorException.Unauthorized) {
                log.error("error getting cities",e)
                return null
            } catch (e: HttpClientErrorException.Forbidden) {
                log.error("error getting cities",e)
                return null
            } catch (e: HttpClientErrorException.NotFound) {
                log.error("error getting cities",e)
                return null
            } catch (e: Exception) {
                log.error("error getting cities, attempt $attempts $mustRetry",e)
                mustRetry = true
                attempts++
                return null
            }
        } while (mustRetry && attempts <= retryCount.toInt())

    }

    override fun saveBranch(data: CreateBranchRequestInput): CreateBranchResponse {
        var attempts = 0
        var mustRetry = false
        activatingLogs()
        var askNewBranchResponse = CreateBranchResponse(HttpStatus.OK)

        do {
            log.info("attempt: $attempts")
            Thread.sleep((5 * 1000 * attempts).toLong())
            try {
                val headers = HttpHeaders()
                headers.set("token", apiToken)
                val httpPayload = HttpEntity(data, headers)
                log.debug("httpPayload: $httpPayload")
                val response =
                    restTemplate.exchange("$apiURL/branches/new", HttpMethod.POST, httpPayload, EntregaloNewBranchResponse::class.java)
                if (response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.CREATED) {
                    val responseBody: EntregaloNewBranchResponse? = response.body
                    if(responseBody != null){
                        askNewBranchResponse = CreateBranchResponse(response.statusCode,responseBody.data.title,responseBody.data.id)
                    }
                }else{
                    askNewBranchResponse = CreateBranchResponse(response.statusCode)
                }

            }catch (e: HttpClientErrorException.BadRequest) {
                log.error("error calling entregalo",e)
                askNewBranchResponse = CreateBranchResponse(HttpStatus.BAD_REQUEST)
            } catch (e: HttpClientErrorException.Unauthorized) {
                log.error("error calling entregalo",e)
                askNewBranchResponse = CreateBranchResponse(HttpStatus.UNAUTHORIZED)
            } catch (e: HttpClientErrorException.Forbidden) {
                log.error("error calling entregalo",e)
                askNewBranchResponse = CreateBranchResponse(HttpStatus.FORBIDDEN)
            } catch (e: HttpClientErrorException.NotFound) {
                log.error("error calling entregalo",e)
                askNewBranchResponse = CreateBranchResponse(HttpStatus.NOT_FOUND)
            } catch (e: Exception) {
                log.error("error calling entregalo, attempt number $attempts",e)
                mustRetry = true
                attempts++
                askNewBranchResponse = CreateBranchResponse(HttpStatus.NOT_FOUND, " Exception: $e")
            }
        }while (mustRetry && attempts <= retryCount.toInt())
        return askNewBranchResponse
    }

    fun activatingLogs(){
        restTemplate.requestFactory = HttpComponentsClientHttpRequestFactory()
    }

}