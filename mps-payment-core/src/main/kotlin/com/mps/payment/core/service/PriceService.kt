package com.mps.payment.core.service

import com.mps.payment.core.model.Discount
import com.mps.payment.core.model.DiscountRule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
class PriceService(
    private val discountRuleService: DiscountRuleService
) {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun getOrderPrice(dropshippingSaleId: UUID, quantity: Int, basePrice: BigDecimal, discountRulesJson: String? = null): BigDecimal {
        return if (discountRulesJson != null) {
            val discountRules = discountRuleService.parseDiscountRules(discountRulesJson)

            if (discountRules.isEmpty()) {
                log.error("Error applying discount rules for dropshippingsale (dropshippingsaleId=${dropshippingSaleId})")
                return getRegularPrice(quantity, basePrice)
            }

            var discount: Discount? = null

            for (discountRule: DiscountRule in discountRules) {
                if (discountRule.accepts(quantity)) {
                    discount = discountRule.discount
                    break
                }
            }

            if (discount != null) {
                applyDiscount(quantity, basePrice, discount)
            } else {
                getRegularPrice(quantity, basePrice)
            }
        } else {
            getRegularPrice(quantity, basePrice)
        }
    }

    private fun getRegularPrice(quantity: Int, basePrice: BigDecimal): BigDecimal {
        return basePrice.multiply(quantity.toBigDecimal())
    }

    private fun applyDiscount(quantity: Int, basePrice: BigDecimal, discount: Discount): BigDecimal {
        return discount.apply(quantity, basePrice)
    }
}