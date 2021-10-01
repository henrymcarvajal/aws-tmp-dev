package com.mps.payment.core.security.policy.dto

import java.time.LocalDateTime

class Environment(
        val time: LocalDateTime
) {
    val attributes: Map<String, Object> = mutableMapOf()
}