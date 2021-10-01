package com.mps.payment.core.security.policy

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.io.Serializable

@Component
class CustomPermissionEvaluator : PermissionEvaluator {

    @Autowired
    private val policyEnforcement: PolicyEnforcement? = null

    override fun hasPermission(authentication: Authentication, targetType: Any, permission: Any): Boolean {
        /*val currentUser = authentication.principal as User
        val subject = Subject(currentUser.id.toString(), currentUser.roles.split(","))
        val resource = Resource(permission as String)
        val environment = Environment(LocalDateTime.now())
        return policyEnforcement!!.check(subject, resource, null, environment)*/
        throw UnsupportedOperationException("Use dafault permission evaluator instead")
    }

    override fun hasPermission(authentication: Authentication, targetId: Serializable,
                               targetType: String, permission: Any): Boolean {
        throw UnsupportedOperationException("Use hasPermission(#id, 'view') instead")
    }
}