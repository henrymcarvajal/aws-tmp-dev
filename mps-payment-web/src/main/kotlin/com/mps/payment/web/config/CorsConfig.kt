package com.mps.payment.web.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter


@Configuration
class CorsConfig {

    @Value("\${app.cors.allow}")
    lateinit var allowOrigin: String

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun corsConfigurer(): WebMvcConfigurer? {
        return object : WebMvcConfigurerAdapter() {
            override fun addCorsMappings(registry: CorsRegistry) {
                log.info("spring mvc cors url $allowOrigin")
                val newUrl = processUrl(allowOrigin)
                if (newUrl != null) {
                    registry.addMapping("/**").allowedOrigins(allowOrigin, newUrl)
                            .allowedMethods("GET", "PUT", "POST", "PATCH", "DELETE", "OPTIONS");
                } else {
                    registry.addMapping("/**").allowedOrigins(allowOrigin)
                            .allowedMethods("GET", "PUT", "POST", "PATCH", "DELETE", "OPTIONS");
                }

            }
        }
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource? {
        log.info("spring security cors url $allowOrigin")
        val configuration = CorsConfiguration()
        val urls = mutableListOf(allowOrigin)
        val newUrl = processUrl(allowOrigin)
        if (newUrl != null) {
            urls.add(newUrl)
        }
        configuration.allowedOrigins = urls
        configuration.allowedMethods = listOf("GET", "POST", "DELETE", "PATCH")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    private fun processUrl(url: String): String? {
        if (url.contains("www")) {
            return url.replace("www.", "")
        }
        return null
    }
}