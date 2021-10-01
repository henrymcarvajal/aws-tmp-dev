package com.mps.payment.core.security.policy

import com.mps.payment.core.security.policy.dto.Environment
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.expression.EvaluationException
import org.springframework.stereotype.Component
import java.util.*


@Component
class AuthorizationPolicyEnforcement(
        private val policyDefinition: PolicyDefinition
) : PolicyEnforcement {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    override fun check(subject: Any?, resource: Any?, action: Any?, environment: Environment?): Boolean {
        val allRules: List<PolicyRule> = policyDefinition.allPolicyRules
        val cxt = SecurityAccessContext(subject, resource, action, environment)
        val matchedRules: List<PolicyRule> = filterRules(allRules, cxt)
        return checkRules(matchedRules, cxt)
    }

    private fun filterRules(allRules: List<PolicyRule>, cxt: SecurityAccessContext): List<PolicyRule> {
        val matchedRules: MutableList<PolicyRule> = ArrayList()
        for (rule in allRules) {
            try {
                if (rule.target!!.getValue(cxt, Boolean::class.java)!!) {
                    matchedRules.add(rule)
                }
            } catch (ex: EvaluationException) {
                log.info("An error occurred while filtering PolicyRule. " + rule.name + ":{" + rule.description + "} $ex")
            }
        }
        return matchedRules
    }

    private fun checkRules(matchedRules: List<PolicyRule>, cxt: SecurityAccessContext): Boolean {
        for (rule in matchedRules) {
            try {
                if (rule.condition!!.getValue(cxt, Boolean::class.java)!!) {
                    return true
                }
            } catch (ex: EvaluationException) {
                log.info("An error occurred while checking PolicyRule. " + rule.name + ":{" + rule.description + "} $ex")
            }
        }
        return false
    }
}