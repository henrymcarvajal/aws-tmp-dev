package com.mps.payment.core.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "jwt")
class JwtProperties {
    val secretKey = "secret"

    //validity in milliseconds
    val validityInMs: Long = 3600000 // 1h

}