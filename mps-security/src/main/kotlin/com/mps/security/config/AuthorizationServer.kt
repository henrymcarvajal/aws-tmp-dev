package com.mps.security.config

import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer
import javax.sql.DataSource

import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer

import org.springframework.security.authentication.AuthenticationManager

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.NoOpPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter

import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer
import java.lang.Exception


@Configuration
@EnableAuthorizationServer
class AuthorizationServer : AuthorizationServerConfigurerAdapter() {

    @Autowired
    private lateinit var dataSource: DataSource

    override fun configure(oauthServer: AuthorizationServerSecurityConfigurer) {
        oauthServer.checkTokenAccess("permitAll()")
    }


    override fun configure(clients: ClientDetailsServiceConfigurer) {
        clients.jdbc(dataSource).passwordEncoder(encoder())
    }

    @Autowired
    private val authenticationManager: AuthenticationManager? = null

    @Throws(Exception::class)
    override fun configure(endpoints: AuthorizationServerEndpointsConfigurer) {
        endpoints.authenticationManager(authenticationManager)
    }

    @Bean
    fun encoder(): BCryptPasswordEncoder {
        return BCryptPasswordEncoder(10)
    }
}