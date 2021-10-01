package com.mps.payment.core.service

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mps.payment.core.model.DiscountRule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.lang.reflect.Type

@Service
class DiscountRuleParser {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun parseDiscountRules(discountRulesJson: String): MutableList<DiscountRule> {
        if (discountRulesJson.isNotBlank()) {
            try {
                val type: Type = object : TypeToken<List<DiscountRule?>?>() {}.type
                return Gson().fromJson(discountRulesJson, type)
            } catch (e: Exception) {
                log.error("Error parsing rules", e)
            }
        }
        return mutableListOf()
    }

    fun unparseDiscountRules(discountRules : List<DiscountRule>) : String {
        val gson = GsonBuilder().create()
        return gson.toJsonTree(discountRules).asJsonArray.toString()
    }
}