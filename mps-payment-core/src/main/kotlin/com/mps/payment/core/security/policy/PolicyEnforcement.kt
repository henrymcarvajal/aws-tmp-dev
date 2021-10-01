package com.mps.payment.core.security.policy

import com.mps.payment.core.security.policy.dto.Environment

interface PolicyEnforcement {
    fun check(subject: Any?, resource: Any?, action: Any?, environment: Environment?): Boolean
}