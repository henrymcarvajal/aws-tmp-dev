package com.mps.payment.core

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling


@SpringBootApplication
@EnableScheduling
class MpsPaymentCoreApplication

fun main(args: Array<String>) {
	runApplication<MpsPaymentCoreApplication>(*args)
}