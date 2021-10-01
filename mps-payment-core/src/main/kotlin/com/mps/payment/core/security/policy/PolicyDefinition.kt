package com.mps.payment.core.security.policy

interface PolicyDefinition {
    val allPolicyRules: List<PolicyRule>
}