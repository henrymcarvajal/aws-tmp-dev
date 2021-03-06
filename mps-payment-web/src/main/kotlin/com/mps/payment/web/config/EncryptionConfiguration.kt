package com.mps.payment.web.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder


@Configuration
class EncryptionConfiguration {

    @Bean
    @Throws(Exception::class)
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

}