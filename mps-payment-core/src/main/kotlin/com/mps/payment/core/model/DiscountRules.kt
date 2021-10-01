package com.mps.payment.core.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.math.BigDecimal
import java.util.stream.Stream
import javax.validation.Valid
import javax.validation.constraints.Min

data class DiscountRule(
    @get:Min(value = 1, message = "quantity must be greater than 0")
    val quantity: Int,
    val condition: DiscountCondition,
    @get:Valid
    val discount: Discount
) {
    fun accepts(quantity: Int): Boolean {
        return when (condition) {
            DiscountCondition.EQUAL -> quantity == this.quantity
            DiscountCondition.GREATER_THAN_EQUAL -> quantity >= this.quantity
            //DiscountCondition.LESS_THAN_EQUAL -> quantity <= this.quantity
        }
    }
}

data class Discount(
    val target: DiscountTarget,
    val type: DiscountType,
    @get:Min(value = 1, message = "amount must be greater than 0")
    val amount: Int
) {
    fun apply(quantity: Int, basePrice: BigDecimal): BigDecimal {
        when (type) {
            DiscountType.PERCENTAGE -> {
                return when (target) {
                    DiscountTarget.UNIT_PRICE -> {
                        val percentage = BigDecimal.valueOf(100L).minus(amount.toBigDecimal()).divide(BigDecimal.valueOf(100L))
                        val unitPrice = basePrice.multiply(percentage)
                        quantity.toBigDecimal().multiply(unitPrice)
                    }
                    DiscountTarget.TOTAL_PRICE -> {
                        val percentage = BigDecimal.valueOf(100L).minus(amount.toBigDecimal()).divide(BigDecimal.valueOf(100L))
                        val regularPrice = quantity.toBigDecimal().multiply(basePrice)
                        regularPrice.multiply(percentage)
                    }
                }
            }

            DiscountType.FIXED_VALUE -> {
                return when (target) {
                    DiscountTarget.UNIT_PRICE -> {
                        val unitPrice = basePrice.minus(amount.toBigDecimal())
                        quantity.toBigDecimal().multiply(unitPrice)
                    }
                    DiscountTarget.TOTAL_PRICE -> {
                        val regularPrice = quantity.toBigDecimal().multiply(basePrice)
                        regularPrice.minus(amount.toBigDecimal())
                    }
                }
            }

            DiscountType.FIXED_PRICE -> {
                return when (target) {
                    DiscountTarget.TOTAL_PRICE -> {
                        amount.toBigDecimal()
                    }
                    else -> quantity.toBigDecimal().multiply(basePrice)
                }
            }
        }
    }
}

enum class DiscountCondition(@get:JsonValue val discountCondition: String) {

    //This ordering affects de evaluation precedence of discount rules
    EQUAL("EQUAL"),
    GREATER_THAN_EQUAL("GREATER_THAN_EQUAL");
    //LESS_THAN_EQUAL("LESS_THAN_EQUAL")

    companion object {
        @JsonCreator
        fun decode(operationName: String): DiscountCondition {
            return Stream.of(*values()).filter { targetEnum: DiscountCondition -> targetEnum.discountCondition == operationName }
                .findFirst().orElse(null)
        }
    }
}

enum class DiscountTarget(@get:JsonValue val discountTarget: String) {
    UNIT_PRICE("UNIT_PRICE"),
    TOTAL_PRICE("TOTAL_PRICE");

    companion object {
        @JsonCreator
        fun decode(discountTarget: String): DiscountTarget {
            return Stream.of(*values()).filter { targetEnum: DiscountTarget -> targetEnum.discountTarget == discountTarget }
                .findFirst().orElse(null)
        }
    }
}

enum class DiscountType(@get:JsonValue val discountType: String) {
    PERCENTAGE("PERCENTAGE"),
    FIXED_VALUE("FIXED_VALUE"),
    FIXED_PRICE("FIXED_PRICE");

    companion object {
        @JsonCreator
        fun decode(discountTarget: String): DiscountType {
            return Stream.of(*values()).filter { targetEnum: DiscountType -> targetEnum.discountType == discountTarget }
                .findFirst().orElse(null)
        }
    }
}