package com.mps.payment.core.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mps.payment.core.model.DiscountCondition
import com.mps.payment.core.model.DiscountRule
import com.mps.payment.core.model.DiscountTarget
import com.mps.payment.core.model.DiscountType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.springframework.util.Assert
import java.lang.reflect.Type
import java.math.BigDecimal
import java.util.*

internal class PriceServiceTest {

    private lateinit var priceService: PriceService

    @Mock
    private lateinit var discountRuleService: DiscountRuleService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        priceService = PriceService(discountRuleService)
    }

    companion object {
        const val NO_DISCOUNT_RULES = "test NO_DISCOUNT_RULES failed"
        const val DISCOUNT_RULE_EQUALS_IS_MET = "test DISCOUNT_RULE_EQUALS_IS_MET failed"
        const val DISCOUNT_RULE_EQUALS_IS_NOT_MET = "test DISCOUNT_RULE_EQUALS_IS_NOT_MET failed"
        const val DISCOUNT_RULE_GREATER_THAN_EQUAL_IS_MET = "test DISCOUNT_RULE_GREATER_THAN_EQUAL_IS_MET failed"
        const val DISCOUNT_RULE_GREATER_THAN_EQUAL_IS_NOT_MET = "test DISCOUNT_RULE_GREATER_THAN_EQUAL_IS_NOT_MET failed"
    }

    private fun getDiscountRules(discountRulesJson: String): List<DiscountRule> {
        if (discountRulesJson.isNotBlank()) {
            val type: Type = object : TypeToken<List<DiscountRule?>?>() {}.type
            val list : List<DiscountRule> = Gson().fromJson(discountRulesJson, type)
            return list.sortedWith(compareBy({ it.condition }, { -it.quantity }))
        }
        return listOf()
    }

    @Test
    fun `get regular price when there are no discount rules`() {
        val quantity = 1
        val basePrice = BigDecimal.valueOf(5000L)
        val result = priceService.getOrderPrice(dropshippingSaleId = UUID.randomUUID(), quantity = quantity, basePrice = basePrice)
        val totalPrice = quantity.toBigDecimal().multiply(basePrice)
        Assert.isTrue(result == totalPrice, NO_DISCOUNT_RULES)
    }

    @Test
    fun `get regular price when discount rule Json is malformed`() {
        val quantity = 5
        val basePrice = BigDecimal.valueOf(5000L)

        val discountQuantity = 5
        val discountCondition = DiscountCondition.EQUAL
        val discountTarget = DiscountTarget.UNIT_PRICE
        val discountType = DiscountType.PERCENTAGE
        val discountAmount = 10
        val discountRulesJson = "[{\n" +
                "    \"quantity\": \"${discountQuantity}\",\n" +
                "    \"condition\": \"${discountCondition}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget}\",\n" +
                "      \"type\": \"${discountType}\",\n" +
                "      \"amount\": \"${discountAmount}\"\n" +
                //"    }\n" +
                "  }]"

        val result =
            priceService.getOrderPrice(dropshippingSaleId = UUID.randomUUID(), quantity = quantity, basePrice = basePrice, discountRulesJson = discountRulesJson)

        val totalPrice = quantity.toBigDecimal().multiply(basePrice)
        Assert.isTrue(result == totalPrice, DISCOUNT_RULE_EQUALS_IS_MET)
    }

    @Test
    fun `get discounted price when discount rule condition EQUALS is met - one rule - scenario 1`() {
        val quantity = 5
        val basePrice = BigDecimal.valueOf(5000L)

        val discountQuantity = 5
        val discountCondition = DiscountCondition.EQUAL
        val discountTarget = DiscountTarget.UNIT_PRICE
        val discountType = DiscountType.PERCENTAGE
        val discountAmount = 10
        val discountRulesJson = "[{\n" +
                "    \"quantity\": \"${discountQuantity}\",\n" +
                "    \"condition\": \"${discountCondition}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget}\",\n" +
                "      \"type\": \"${discountType}\",\n" +
                "      \"amount\": \"${discountAmount}\"\n" +
                "    }\n" +
                "  }]"

        val list = getDiscountRules(discountRulesJson)
        Mockito.`when`(discountRuleService.parseDiscountRules(discountRulesJson)).thenReturn(list)

        val result =
            priceService.getOrderPrice(dropshippingSaleId = UUID.randomUUID(), quantity = quantity, basePrice = basePrice, discountRulesJson = discountRulesJson)
        val percentage = (BigDecimal.valueOf(100L).minus(BigDecimal.valueOf(discountAmount.toLong()))).divide(BigDecimal.valueOf(100L))
        val unitPrice = basePrice.multiply(percentage)
        val totalPrice = quantity.toBigDecimal().multiply(unitPrice)
        Assert.isTrue(result == totalPrice, DISCOUNT_RULE_EQUALS_IS_MET)
    }

    @Test
    fun `get discounted price when discount rule condition EQUALS is met - one rule - scenario 2`() {
        val quantity = 5
        val basePrice = BigDecimal.valueOf(5000L)

        val discountQuantity = 5
        val discountCondition = DiscountCondition.EQUAL
        val discountTarget = DiscountTarget.TOTAL_PRICE
        val discountType = DiscountType.PERCENTAGE
        val discountAmount = 5
        val discountRulesJson = "[{\n" +
                "    \"quantity\": \"${discountQuantity}\",\n" +
                "    \"condition\": \"${discountCondition}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget}\",\n" +
                "      \"type\": \"${discountType}\",\n" +
                "      \"amount\": \"${discountAmount}\"\n" +
                "    }\n" +
                "  }]"

        val list = getDiscountRules(discountRulesJson)
        Mockito.`when`(discountRuleService.parseDiscountRules(discountRulesJson)).thenReturn(list)

        val result =
            priceService.getOrderPrice(dropshippingSaleId = UUID.randomUUID(), quantity = quantity, basePrice = basePrice, discountRulesJson = discountRulesJson)
        val percentage = (BigDecimal.valueOf(100L).minus(BigDecimal.valueOf(discountAmount.toLong()))).divide(BigDecimal.valueOf(100L))
        val unitPrice = basePrice.multiply(percentage)
        val totalPrice = quantity.toBigDecimal().multiply(unitPrice)
        Assert.isTrue(result == totalPrice, DISCOUNT_RULE_EQUALS_IS_MET)
    }

    @Test
    fun `get discounted price when discount rule condition EQUALS is met - one rule - scenario 3`() {
        val quantity = 5
        val basePrice = BigDecimal.valueOf(5000L)

        val discountQuantity = 5
        val discountCondition = DiscountCondition.EQUAL
        val discountTarget = DiscountTarget.UNIT_PRICE
        val discountType = DiscountType.FIXED_VALUE
        val discountAmount = 500
        val discountRulesJson = "[{\n" +
                "    \"quantity\": \"${discountQuantity}\",\n" +
                "    \"condition\": \"${discountCondition}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget}\",\n" +
                "      \"type\": \"${discountType}\",\n" +
                "      \"amount\": \"${discountAmount}\"\n" +
                "    }\n" +
                "  }]"

        val list = getDiscountRules(discountRulesJson)
        Mockito.`when`(discountRuleService.parseDiscountRules(discountRulesJson)).thenReturn(list)

        val unitPrice = basePrice.minus(discountAmount.toBigDecimal())
        val totalPrice = quantity.toBigDecimal().multiply(unitPrice)

        val result =
            priceService.getOrderPrice(dropshippingSaleId = UUID.randomUUID(), quantity = quantity, basePrice = basePrice, discountRulesJson = discountRulesJson)

        println (result)
        Assert.isTrue(result == totalPrice, DISCOUNT_RULE_EQUALS_IS_MET)
    }

    @Test
    fun `get discounted price when discount rule condition EQUALS is met - one rule - scenario 4`() {
        val quantity = 5
        val basePrice = BigDecimal.valueOf(5000L)

        val discountQuantity = 5
        val discountCondition = DiscountCondition.EQUAL
        val discountTarget = DiscountTarget.TOTAL_PRICE
        val discountType = DiscountType.FIXED_VALUE
        val discountAmount = 10000
        val discountRulesJson = "[{\n" +
                "    \"quantity\": \"${discountQuantity}\",\n" +
                "    \"condition\": \"${discountCondition}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget}\",\n" +
                "      \"type\": \"${discountType}\",\n" +
                "      \"amount\": \"${discountAmount}\"\n" +
                "    }\n" +
                "  }]"

        val list = getDiscountRules(discountRulesJson)
        Mockito.`when`(discountRuleService.parseDiscountRules(discountRulesJson)).thenReturn(list)

        val result =
            priceService.getOrderPrice(dropshippingSaleId = UUID.randomUUID(), quantity = quantity, basePrice = basePrice, discountRulesJson = discountRulesJson)

        val regularPrice = quantity.toBigDecimal().multiply(basePrice)
        val totalPrice = regularPrice.minus(discountAmount.toBigDecimal())
        Assert.isTrue(result == totalPrice, DISCOUNT_RULE_EQUALS_IS_MET)
    }

    @Test
    fun `get discounted price when discount rule condition EQUALS is met - one rule - scenario 5`() {
        val quantity = 5
        val basePrice = BigDecimal.valueOf(5000L)

        val discountQuantity = 5
        val discountCondition = DiscountCondition.EQUAL
        val discountTarget = DiscountTarget.TOTAL_PRICE
        val discountType = DiscountType.FIXED_PRICE
        val discountAmount = 5000
        val discountRulesJson = "[{\n" +
                "    \"quantity\": \"${discountQuantity}\",\n" +
                "    \"condition\": \"${discountCondition}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget}\",\n" +
                "      \"type\": \"${discountType}\",\n" +
                "      \"amount\": \"${discountAmount}\"\n" +
                "    }\n" +
                "  }]"

        val list = getDiscountRules(discountRulesJson)
        Mockito.`when`(discountRuleService.parseDiscountRules(discountRulesJson)).thenReturn(list)

        val result =
            priceService.getOrderPrice(dropshippingSaleId = UUID.randomUUID(), quantity = quantity, basePrice = basePrice, discountRulesJson = discountRulesJson)


        Assert.isTrue(result == discountAmount.toBigDecimal(), DISCOUNT_RULE_EQUALS_IS_MET)
    }

    @Test
    fun `get discounted price when discount rule condition EQUALS is met - more than one rule`() {
        val quantity = 5
        val basePrice = BigDecimal.valueOf(5000L)

        val discountQuantity1 = 5
        val discountCondition1 = DiscountCondition.EQUAL
        val discountTarget1 = DiscountTarget.UNIT_PRICE
        val discountType1 = DiscountType.PERCENTAGE
        val discountAmount1 = 10

        val discountQuantity2 = 6
        val discountCondition2 = DiscountCondition.GREATER_THAN_EQUAL
        val discountTarget2 = DiscountTarget.UNIT_PRICE
        val discountType2 = DiscountType.PERCENTAGE
        val discountAmount2 = 15

        val discountRulesJson = "[{\n" +
                "    \"quantity\": \"${discountQuantity1}\",\n" +
                "    \"condition\": \"${discountCondition1}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget1}\",\n" +
                "      \"type\": \"${discountType1}\",\n" +
                "      \"amount\": \"${discountAmount1}\"\n" +
                "    }\n" +
                "  }," +
                "{\n" +
                "    \"quantity\": \"${discountQuantity2}\",\n" +
                "    \"condition\": \"${discountCondition2}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget2}\",\n" +
                "      \"type\": \"${discountType2}\",\n" +
                "      \"amount\": \"${discountAmount2}\"\n" +
                "    }\n" +
                "  }]"

        val list = getDiscountRules(discountRulesJson)
        Mockito.`when`(discountRuleService.parseDiscountRules(discountRulesJson)).thenReturn(list)

        val result =
            priceService.getOrderPrice(dropshippingSaleId = UUID.randomUUID(), quantity = quantity, basePrice = basePrice, discountRulesJson = discountRulesJson)
        val percentage = (BigDecimal.valueOf(100L).minus(BigDecimal.valueOf(discountAmount1.toLong()))).divide(BigDecimal.valueOf(100L))
        val unitPrice = basePrice.multiply(percentage)
        val totalPrice = quantity.toBigDecimal().multiply(unitPrice)
        Assert.isTrue(result == totalPrice, DISCOUNT_RULE_EQUALS_IS_MET)
    }

    @Test
    fun `get discounted price when discount rule condition GREATER_THAN_EQUAL is met - more than one rule - rule precedence`() {
        val quantity = 25
        val basePrice = BigDecimal.valueOf(5000L)

        val discountQuantity1 = 5
        val discountCondition1 = DiscountCondition.GREATER_THAN_EQUAL
        val discountTarget1 = DiscountTarget.TOTAL_PRICE
        val discountType1 = DiscountType.PERCENTAGE
        val discountAmount1 = 10

        val discountQuantity2 = 10
        val discountCondition2 = DiscountCondition.GREATER_THAN_EQUAL
        val discountTarget2 = DiscountTarget.TOTAL_PRICE
        val discountType2 = DiscountType.PERCENTAGE
        val discountAmount2 = 15

        val discountQuantity3 = 20
        val discountCondition3 = DiscountCondition.GREATER_THAN_EQUAL
        val discountTarget3 = DiscountTarget.TOTAL_PRICE
        val discountType3 = DiscountType.PERCENTAGE
        val discountAmount3 = 20

        val discountQuantity4 = 2
        val discountCondition4 = DiscountCondition.EQUAL
        val discountTarget4 = DiscountTarget.TOTAL_PRICE
        val discountType4 = DiscountType.PERCENTAGE
        val discountAmount4 = 5

        val discountQuantity5 = 3
        val discountCondition5 = DiscountCondition.EQUAL
        val discountTarget5 = DiscountTarget.TOTAL_PRICE
        val discountType5 = DiscountType.PERCENTAGE
        val discountAmount5 = 6

        val discountQuantity6 = 4
        val discountCondition6 = DiscountCondition.EQUAL
        val discountTarget6 = DiscountTarget.TOTAL_PRICE
        val discountType6 = DiscountType.PERCENTAGE
        val discountAmount6 = 7

        val discountRulesJson = "[{\n" +
                "    \"quantity\": \"${discountQuantity1}\",\n" +
                "    \"condition\": \"${discountCondition1}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget1}\",\n" +
                "      \"type\": \"${discountType1}\",\n" +
                "      \"amount\": \"${discountAmount1}\"\n" +
                "    }\n" +
                "  }," +
                "{\n" +
                "    \"quantity\": \"${discountQuantity6}\",\n" +
                "    \"condition\": \"${discountCondition6}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget6}\",\n" +
                "      \"type\": \"${discountType6}\",\n" +
                "      \"amount\": \"${discountAmount6}\"\n" +
                "    }\n" +
                "  }," +
                "{\n" +
                "    \"quantity\": \"${discountQuantity4}\",\n" +
                "    \"condition\": \"${discountCondition4}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget4}\",\n" +
                "      \"type\": \"${discountType4}\",\n" +
                "      \"amount\": \"${discountAmount4}\"\n" +
                "    }\n" +
                "  }," +
                "{\n" +
                "    \"quantity\": \"${discountQuantity3}\",\n" +
                "    \"condition\": \"${discountCondition3}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget3}\",\n" +
                "      \"type\": \"${discountType3}\",\n" +
                "      \"amount\": \"${discountAmount3}\"\n" +
                "    }\n" +
                "  }," +
                "{\n" +
                "    \"quantity\": \"${discountQuantity5}\",\n" +
                "    \"condition\": \"${discountCondition5}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget5}\",\n" +
                "      \"type\": \"${discountType5}\",\n" +
                "      \"amount\": \"${discountAmount5}\"\n" +
                "    }\n" +
                "  }," +
                "{\n" +
                "    \"quantity\": \"${discountQuantity2}\",\n" +
                "    \"condition\": \"${discountCondition2}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget2}\",\n" +
                "      \"type\": \"${discountType2}\",\n" +
                "      \"amount\": \"${discountAmount2}\"\n" +
                "    }\n" +
                "  }]"

        val list = getDiscountRules(discountRulesJson)
        Mockito.`when`(discountRuleService.parseDiscountRules(discountRulesJson)).thenReturn(list)

        val result = priceService.getOrderPrice(
            dropshippingSaleId = UUID.randomUUID(),
            quantity = quantity,
            basePrice = basePrice,
            discountRulesJson = discountRulesJson
        )
        val percentage = (BigDecimal.valueOf(100L).minus(BigDecimal.valueOf(discountAmount3.toLong()))).divide(BigDecimal.valueOf(100L))
        val unitPrice = basePrice.multiply(percentage)
        val totalPrice = quantity.toBigDecimal().multiply(unitPrice)
        Assert.isTrue(result == totalPrice, DISCOUNT_RULE_GREATER_THAN_EQUAL_IS_MET)
    }

    @Test
    fun `get regular price when discount rule condition EQUALS is NOT met - one rule`() {
        val quantity = 2
        val basePrice = BigDecimal.valueOf(5000L)

        val discountQuantity = 5
        val discountCondition = DiscountCondition.EQUAL
        val discountTarget = DiscountTarget.UNIT_PRICE
        val discountType = DiscountType.PERCENTAGE
        val discountAmount = 10
        val discountRuleJson = "[{\n" +
                "    \"quantity\": \"${discountQuantity}\",\n" +
                "    \"condition\": \"${discountCondition}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget}\",\n" +
                "      \"type\": \"${discountType}\",\n" +
                "      \"amount\": \"${discountAmount}\"\n" +
                "    }\n" +
                "  }]"
        val result =
            priceService.getOrderPrice(dropshippingSaleId = UUID.randomUUID(), quantity = quantity, basePrice = basePrice, discountRulesJson = discountRuleJson)

        val totalPrice = quantity.toBigDecimal().multiply(basePrice)
        Assert.isTrue(result == totalPrice, DISCOUNT_RULE_EQUALS_IS_NOT_MET)
    }

    @Test
    fun `get discounted price when discount rule condition GREATER_THAN_EQUAL is met - one rule`() {
        val quantity = 10
        val basePrice = BigDecimal.valueOf(5000L)

        val discountQuantity = 5
        val discountCondition = DiscountCondition.GREATER_THAN_EQUAL
        val discountTarget = DiscountTarget.UNIT_PRICE
        val discountType = DiscountType.PERCENTAGE
        val discountAmount = 10
        val discountRuleJson = "[{\n" +
                "    \"quantity\": \"${discountQuantity}\",\n" +
                "    \"condition\": \"${discountCondition}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget}\",\n" +
                "      \"type\": \"${discountType}\",\n" +
                "      \"amount\": \"${discountAmount}\"\n" +
                "    }\n" +
                "  }]"

        val list = getDiscountRules(discountRuleJson)
        Mockito.`when`(discountRuleService.parseDiscountRules(discountRuleJson)).thenReturn(list)

        val result =
            priceService.getOrderPrice(dropshippingSaleId = UUID.randomUUID(), quantity = quantity, basePrice = basePrice, discountRulesJson = discountRuleJson)
        val percentage = (BigDecimal.valueOf(100L).minus(BigDecimal.valueOf(discountAmount.toLong()))).divide(BigDecimal.valueOf(100L))
        val unitPrice = basePrice.multiply(percentage)
        val totalPrice = quantity.toBigDecimal().multiply(unitPrice)
        Assert.isTrue(result == totalPrice, DISCOUNT_RULE_GREATER_THAN_EQUAL_IS_MET)
    }

    @Test
    fun `get regular price when discount rule condition GREATER_THAN_EQUAL is NOT met - one rule`() {
        val quantity = 2
        val basePrice = BigDecimal.valueOf(5000L)

        val discountQuantity = 5
        val discountCondition = DiscountCondition.GREATER_THAN_EQUAL
        val discountTarget = DiscountTarget.UNIT_PRICE
        val discountType = DiscountType.PERCENTAGE
        val discountAmount = 10
        val discountRuleJson = "[{\n" +
                "    \"quantity\": \"${discountQuantity}\",\n" +
                "    \"condition\": \"${discountCondition}\",\n" +
                "    \"discount\": {\n" +
                "      \"target\": \"${discountTarget}\",\n" +
                "      \"type\": \"${discountType}\",\n" +
                "      \"amount\": \"${discountAmount}\"\n" +
                "    }\n" +
                "  }]"
        val result =
            priceService.getOrderPrice(dropshippingSaleId = UUID.randomUUID(), quantity = quantity, basePrice = basePrice, discountRulesJson = discountRuleJson)
        val totalPrice = quantity.toBigDecimal().multiply(basePrice)
        Assert.isTrue(result == totalPrice, DISCOUNT_RULE_GREATER_THAN_EQUAL_IS_NOT_MET)
    }
}

