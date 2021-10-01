package com.mps.payment.core.security.policy

import com.mps.payment.core.model.User
import com.mps.payment.core.security.policy.dto.Environment
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ContextAwarePolicyEnforcement(
        var policy: PolicyEnforcement
) {

    fun checkPermission(resource: Any?, permission: String?) {
        //Getting the subject
        val auth: Authentication = SecurityContextHolder.getContext().authentication
        val principal = auth.principal as User

        //Getting the environment
        val environment = Environment(LocalDateTime.now())

        if (!policy.check(principal, resource, permission, environment))
            throw AccessDeniedException("Access xxxxxxx denied")
    }

}