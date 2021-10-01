package com.mps.security.controller

import com.mps.common.auth.request.IntegrationInformation
import com.mps.common.auth.request.IntegrationRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.provider.NoSuchClientException
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import java.security.Principal
import org.springframework.security.oauth2.provider.client.BaseClientDetails
import org.springframework.web.bind.annotation.RequestBody
import java.util.*


@RestController
@EnableResourceServer
class ClientCredentialsController(private val jdbcClientDetailsService: JdbcClientDetailsService) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var encoder: BCryptPasswordEncoder

    @RequestMapping("/validateUser")
    fun user(user: Principal?): Principal? {
        return user
    }

    @PostMapping("/integration")
    fun createIntegration(@RequestBody request: IntegrationRequest): ResponseEntity<*> {
        val existingRecord = try {
            jdbcClientDetailsService.loadClientByClientId(request.merchantID)
        } catch (e: NoSuchClientException) {
            log.info("there is no credentials for merchant ${request.merchantID}")
            null
        }
        return try {
            if (existingRecord != null) {
                jdbcClientDetailsService.removeClientDetails(request.merchantID)
            }
            val clientDetails = BaseClientDetails(request.merchantID, request.resource, request.scopes, request.grantType, null)
            val secret = UUID.randomUUID().toString()
            clientDetails.clientSecret = secret
            jdbcClientDetailsService.setPasswordEncoder(encoder)
            jdbcClientDetailsService.addClientDetails(clientDetails)
            ResponseEntity.ok(IntegrationInformation(
                    clientId = request.merchantID,
                    publicKey = request.merchantID.take(6),
                    secret = secret
            ))
        } catch (e: Exception) {
            log.error("error creating credentials {}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creando credenciales")
        }
    }
}