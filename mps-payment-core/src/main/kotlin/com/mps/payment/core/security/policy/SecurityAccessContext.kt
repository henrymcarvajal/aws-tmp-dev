package com.mps.payment.core.security.policy

import com.mps.payment.core.security.policy.dto.Environment


data class SecurityAccessContext(
        val subject: Any?,
        val resource: Any?,
        val action: Any?,
        val environment: Environment?
)