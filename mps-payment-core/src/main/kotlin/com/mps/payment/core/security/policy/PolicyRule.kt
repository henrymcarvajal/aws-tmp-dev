package com.mps.payment.core.security.policy

import org.springframework.expression.Expression

class PolicyRule {

    var target: Expression? = null
    var condition: Expression? = null
    var name: String? = null
    var description: String? = null
}