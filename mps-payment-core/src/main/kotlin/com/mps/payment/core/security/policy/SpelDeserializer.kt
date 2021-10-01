package com.mps.payment.core.security.policy

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import org.springframework.expression.Expression
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class SpelDeserializer protected constructor(vc: Class<*>?) : StdDeserializer<Expression?>(vc) {
    constructor() : this(null) {}

    @Throws(IOException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Expression {
        val expressionString = jp.codec.readValue(jp, String::class.java)
        val spelExpressionParser = SpelExpressionParser()
        return spelExpressionParser.parseExpression(expressionString)
    }
}