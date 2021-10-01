package com.mps.payment.web

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EntityScan(basePackages = ["com.mps.payment.core"])
@EnableJpaRepositories(basePackages = ["com.mps.payment.core"])
@ComponentScan(basePackages = ["com.mps.payment.core", "com.mps.payment.web"])
@EnableScheduling
class MpsPaymentWebApplication

fun main(args: Array<String>) {
    runApplication<MpsPaymentWebApplication>(*args)
}