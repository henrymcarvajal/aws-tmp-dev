package com.mps.payment.pull.col

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer

@EnableResourceServer
@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
@EnableScheduling
class MpsPullColApplication

fun main(args: Array<String>) {
    runApplication<MpsPullColApplication>(*args)
}