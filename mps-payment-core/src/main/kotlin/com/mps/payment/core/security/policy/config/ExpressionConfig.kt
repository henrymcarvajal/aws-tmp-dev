package com.mps.payment.core.security.policy

import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class ExpressionConfig {
    val rules: MutableList<AuthorizationExpression> = mutableListOf()

    @PostConstruct
    private fun init() {
        var rule = AuthorizationExpression(
                "Admin has all permits",
                "Admin has all permits",
                "subject.roles.contains('ROLE_ADMIN')",
                "true"
        )
        rules.add(rule)

        rule = AuthorizationExpression(
                "Merchant can read only its own information",
                "Merchant can read only its own information",
                "subject.roles.contains('ROLE_MERCHANT') && action == 'MERCHANT_READ'",
                "resource.idUser == subject.id"
        )
        rules.add(rule)

        rule = AuthorizationExpression(
                "Merchant or Customer may read Payment",
                "Merchant or Customer may read Payment",
                "(subject.roles.contains('ROLE_MERCHANT') || subject.roles.contains('ROLE_COSTUMER')) && action == 'PAYMENT_READ'",
                "resource.idMerchant == subject.id || resource.idCustomer == subject.id"
        )
        rules.add(rule)

        rule = AuthorizationExpression(
                "Customer can read only its own information",
                "Customer can read only its own information",
                "subject.roles.contains('ROLE_CUSTOMER') && action == 'CUSTOMER_READ'",
                "resource.idUser == subject.id"
        )

        rules.add(rule)
    }
}