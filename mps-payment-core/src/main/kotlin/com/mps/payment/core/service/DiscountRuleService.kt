package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.ServiceResponse
import com.mps.payment.core.model.DiscountRule
import com.mps.payment.core.model.DropshippingSaleDTO
import com.mps.payment.core.model.toDTO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*


@Service
class DiscountRuleService(
    private val dropshippingSaleService: DropshippingSaleService,
    private val discountRuleParser: DiscountRuleParser
) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun findDiscountRulesById(dropshippingSaleId: UUID): ServiceResponse<*> {
        val dropshippingSaleSearch = dropshippingSaleService.findById(dropshippingSaleId)
        return if (dropshippingSaleSearch.isPresent) {
            var list = listOf<DiscountRule>()
            val dropshippingSale = dropshippingSaleSearch.get()
            if (dropshippingSale.specialConditions != null) {
                list = discountRuleParser.parseDiscountRules(dropshippingSale.specialConditions!!)
                list = orderRulesByPrecedence(list)
            }
            ServiceResponse.Success(list)
        } else {
            ServiceResponse.Error("DropshippingSale not found (id=${dropshippingSaleId})")
        }
    }

    fun updateDiscountRulesById(dropshippingSaleId: UUID, discountRules: List<DiscountRule>): ServiceResponse<*> {
        val dropshippingSaleSearch = dropshippingSaleService.findById(dropshippingSaleId)
        return if (dropshippingSaleSearch.isPresent) {
            val dropshippingSale = dropshippingSaleSearch.get()

            if (dropshippingSale.specialConditions != null) {
                val existingDiscountRules = discountRuleParser.parseDiscountRules(dropshippingSale.specialConditions!!)
                if (existingDiscountRules.isNotEmpty()) {
                    val toReplace = existingDiscountRules.find { it.quantity == discountRules[0].quantity }
                    if (toReplace == null) {
                        existingDiscountRules.add(discountRules[0])
                    } else {
                        val index = existingDiscountRules.indexOf(toReplace)
                        existingDiscountRules[index] = discountRules[0]
                    }
                    dropshippingSale.specialConditions = discountRuleParser.unparseDiscountRules(existingDiscountRules)
                } else {
                    dropshippingSale.specialConditions = discountRuleParser.unparseDiscountRules(discountRules)
                }
            } else {
                dropshippingSale.specialConditions = discountRuleParser.unparseDiscountRules(discountRules)
            }

            return when (val serviceResponse = dropshippingSaleService.updateDropshippingSale(dropshippingSale.toDTO())) {
                is GenericResponse.SuccessResponse -> ServiceResponse.Success(serviceResponse.objP as DropshippingSaleDTO)
                is GenericResponse.ErrorResponse -> ServiceResponse.Error(serviceResponse.message)
            }
        } else {
            ServiceResponse.Error("DropshippingSale not found (id=${dropshippingSaleId})")
        }
    }

    fun parseDiscountRules(discountRulesJson: String): List<DiscountRule> {
        return discountRuleParser.parseDiscountRules(discountRulesJson)
    }

    private fun orderRulesByPrecedence(discountRules: List<DiscountRule>): List<DiscountRule> {
        return discountRules.sortedWith(compareBy({ it.condition }, { -it.quantity }))
    }
}