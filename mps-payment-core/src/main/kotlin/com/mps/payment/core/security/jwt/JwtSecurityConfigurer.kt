package com.mps.payment.core.security.jwt

import com.mps.payment.core.web.WebResponseWriter
import org.springframework.security.config.annotation.SecurityConfigurerAdapter
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

class JwtSecurityConfigurer(
        private val jwtTokenProvider: JwtTokenProvider,
        private val responseWriter: WebResponseWriter
) : SecurityConfigurerAdapter<DefaultSecurityFilterChain?, HttpSecurity>() {

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        val customFilter = JwtTokenAuthenticationFilter(jwtTokenProvider, responseWriter)
        http.addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter::class.java)
    }

}