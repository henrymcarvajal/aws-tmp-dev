package com.mps.payment.core.security.policy


data class AuthorizationExpression
(
        val name: String,
        val description: String,
        val target: String,
        val condition: String
)
