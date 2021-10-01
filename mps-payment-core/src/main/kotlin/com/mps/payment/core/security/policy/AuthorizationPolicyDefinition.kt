package com.mps.payment.core.security.policy

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.expression.Expression
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.*
import javax.annotation.PostConstruct

@Component
class AuthorizationPolicyDefinition(
        private val expressionConfig: ExpressionConfig,
        private val spelDeserializer: SpelDeserializer
) : PolicyDefinition {

    private var rules: List<PolicyRule> = listOf()

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    private fun init() {
        val mapper = ObjectMapper()
        val module = SimpleModule()
        module.addDeserializer(Expression::class.java, spelDeserializer)
        mapper.registerModule(module)
        try {
            var rulesArray: Array<PolicyRule>? = null
            if (expressionConfig != null) {
                val gson = Gson()
                rulesArray = mapper.readValue(gson.toJson(expressionConfig.rules), Array<PolicyRule>::class.java)
            }
            rules = if (rulesArray != null) Arrays.asList(*rulesArray) else emptyList()
        } catch (jme: JsonMappingException) {
            log.info("An JsonMappingException error occurred while parsing the policy file. $jme")
        } catch (ioe: IOException) {
            log.info("An IOException error occurred while reading the policy file. $ioe")
        }
    }

    override val allPolicyRules: List<PolicyRule>
        get() = rules
}