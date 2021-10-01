package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.ServiceResponse
import com.mps.payment.core.model.*
import com.mps.payment.core.util.createMerchantTest
import com.mps.payment.core.util.createProductTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.springframework.util.Assert
import java.util.*

internal class DiscountRuleServiceTest {

    @Mock
    private lateinit var dropshippingSaleService: DropshippingSaleService

    @Mock
    private lateinit var discountRuleParser: DiscountRuleParser

    private lateinit var discountRuleService: DiscountRuleService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        discountRuleService = DiscountRuleService(dropshippingSaleService, discountRuleParser)
    }

    @Test
    fun `There are no discount rules`() {
        val discounts = discountRuleService.parseDiscountRules("")
        Assert.isTrue(discounts.isEmpty(), "Test 'There are no discount rules' failed")
    }

    @Test
    fun `There are no discount rules when JSON malformed - scenario 1`() {
        val discounts = discountRuleService.parseDiscountRules("{/}")
        Assert.isTrue(discounts.isEmpty(), "Test 'There are no discount rules when JSON malformed - scenario 1' failed")
    }

    @Test
    fun `There are no discount rules when JSON malformed - scenario 2`() {
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

        val discounts = discountRuleService.parseDiscountRules(discountRulesJson)
        Assert.isTrue(discounts.isEmpty(), "Test 'There are no discount rules when JSON malformed - scenario 2' failed")
    }

    @Test
    fun `There is only one discount rule`() {
        val discountQuantity = 5
        val discountCondition = DiscountCondition.EQUAL
        val discountTarget = DiscountTarget.UNIT_PRICE
        val discountType = DiscountType.PERCENTAGE
        val discountAmount = 10
        val discountRule =
            DiscountRule(
                quantity = discountQuantity,
                condition = discountCondition,
                discount = Discount(target = discountTarget, type = discountType, amount = discountAmount)
            )
        val discountRulesJson = createDiscountRulesJson(listOf(discountRule))

        Mockito.`when`(discountRuleParser.parseDiscountRules(discountRulesJson)).thenReturn(mutableListOf(discountRule))

        val result = discountRuleService.parseDiscountRules(discountRulesJson)
        Assert.isTrue(result.isNotEmpty() && result[0] == discountRule, "Test 'There is only one discount rule' failed")
    }

    @Test
    fun `There are two rules - scenario 1 - no effect of precedence`() {

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

        val discountRule1 =
            DiscountRule(
                quantity = discountQuantity1,
                condition = discountCondition1,
                discount = Discount(target = discountTarget1, type = discountType1, amount = discountAmount1)
            )
        val discountRule2 =
            DiscountRule(
                quantity = discountQuantity2,
                condition = discountCondition2,
                discount = Discount(target = discountTarget2, type = discountType2, amount = discountAmount2)
            )

        val discountRules = ArrayList<DiscountRule>()
        discountRules.add(discountRule1)
        discountRules.add(discountRule2)
        val discountRulesJson = createDiscountRulesJson(discountRules)

        Mockito.`when`(discountRuleParser.parseDiscountRules(discountRulesJson)).thenReturn(discountRules)

        val result = discountRuleService.parseDiscountRules(discountRulesJson = discountRulesJson)
        Assert.isTrue(result.isNotEmpty() && (result == discountRules), "Test 'There are two rules - scenario 1 - no effect of precedence' failed")
    }

    @Test
    fun `There are two rules - scenario 2 - effect of precedence`() {

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

        val discountRule1 =
            DiscountRule(
                quantity = discountQuantity1,
                condition = discountCondition1,
                discount = Discount(target = discountTarget1, type = discountType1, amount = discountAmount1)
            )
        val discountRule2 =
            DiscountRule(
                quantity = discountQuantity2,
                condition = discountCondition2,
                discount = Discount(target = discountTarget2, type = discountType2, amount = discountAmount2)
            )

        val discountRules = ArrayList<DiscountRule>()
        discountRules.add(discountRule1)
        discountRules.add(discountRule2)

        val discountRulesJson = createDiscountRulesJson(discountRules)

        Mockito.`when`(discountRuleParser.parseDiscountRules(discountRulesJson)).thenReturn(discountRules)

        val result =
            discountRuleService.parseDiscountRules(discountRulesJson = discountRulesJson)
        Assert.isTrue(result.isNotEmpty() && (result == discountRules), "Test 'There are two rules - scenario 2 - effect of precedence' failed")
    }

    @Test
    fun `There are more than two rules - effect of precedence`() {
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

        val discountRule1 = DiscountRule(discountQuantity1, discountCondition1, Discount(discountTarget1, discountType1, discountAmount1))
        val discountRule2 = DiscountRule(discountQuantity2, discountCondition2, Discount(discountTarget2, discountType2, discountAmount2))
        val discountRule3 = DiscountRule(discountQuantity3, discountCondition3, Discount(discountTarget3, discountType3, discountAmount3))
        val discountRule4 = DiscountRule(discountQuantity4, discountCondition4, Discount(discountTarget4, discountType4, discountAmount4))
        val discountRule5 = DiscountRule(discountQuantity5, discountCondition5, Discount(discountTarget5, discountType5, discountAmount5))
        val discountRule6 = DiscountRule(discountQuantity6, discountCondition6, Discount(discountTarget6, discountType6, discountAmount6))

        val discountRules = ArrayList<DiscountRule>()
        discountRules.add(discountRule6)
        discountRules.add(discountRule5)
        discountRules.add(discountRule4)
        discountRules.add(discountRule3)
        discountRules.add(discountRule2)
        discountRules.add(discountRule1)

        val discountRulesJson = createDiscountRulesJson(discountRules)

        Mockito.`when`(discountRuleParser.parseDiscountRules(discountRulesJson)).thenReturn(discountRules)

        val result = discountRuleService.parseDiscountRules(discountRulesJson = discountRulesJson)
        Assert.isTrue(result.isNotEmpty() && (result == discountRules), "Test 'There are more than two rules - effect of precedence' failed")
    }

    @Test
    fun `Update discount rule - no previous rule`() {
        val discountQuantity = 5
        val discountCondition = DiscountCondition.EQUAL
        val discountTarget = DiscountTarget.UNIT_PRICE
        val discountType = DiscountType.PERCENTAGE
        val discountAmount = 10
        val discountRule =
            DiscountRule(
                quantity = discountQuantity,
                condition = discountCondition,
                discount = Discount(target = discountTarget, type = discountType, amount = discountAmount)
            )
        val discountRulesJson = createDiscountRulesJson(listOf(discountRule))

        val id = UUID.randomUUID()
        val dropshippingSaleOptional = createOptionalDropshippingSaleWithNoDiscount(id)
        val dropshippingSale = dropshippingSaleOptional.get()
        val dropshippingSaleDTO = dropshippingSale.toDTO()

        val newDropshippingSaleDTO = dropshippingSaleDTO.copy()
        newDropshippingSaleDTO.specialConditions = discountRulesJson

        val genericResponse = GenericResponse.SuccessResponse(newDropshippingSaleDTO)
        val serviceResponse = ServiceResponse.Success(newDropshippingSaleDTO)
        Mockito.`when`(dropshippingSaleService.findById(id)).thenReturn(dropshippingSaleOptional)
        Mockito.`when`(dropshippingSaleService.updateDropshippingSale(newDropshippingSaleDTO)).thenReturn(genericResponse)
        Mockito.`when`(discountRuleParser.unparseDiscountRules(mutableListOf(discountRule))).thenReturn(discountRulesJson)

        val result = discountRuleService.updateDiscountRulesById(id, listOf(discountRule))
        Assert.isTrue(result == serviceResponse, "Test 'Update discount rule - no previous rule' failed")
    }

    @Test
    fun `Update discount rule - one previous rule - match`() {
        val discountQuantity = 5
        val discountCondition = DiscountCondition.EQUAL
        val discountTarget = DiscountTarget.TOTAL_PRICE
        val discountType = DiscountType.FIXED_PRICE
        val oldDiscountAmount = 100
        val oldDiscountRule =
            DiscountRule(
                quantity = discountQuantity,
                condition = discountCondition,
                discount = Discount(target = discountTarget, type = discountType, amount = oldDiscountAmount)
            )
        val oldDiscountRulesJson = createDiscountRulesJson(listOf(oldDiscountRule))

        val newDiscountAmount = 10000
        val newDiscountRule =
            DiscountRule(
                quantity = discountQuantity,
                condition = discountCondition,
                discount = Discount(target = discountTarget, type = discountType, amount = newDiscountAmount)
            )
        val newDiscountRulesJson = createDiscountRulesJson(listOf(newDiscountRule))

        val id = UUID.randomUUID()
        val dropshippingSaleOptional = createOptionalDropshippingSaleWithDiscounts(id, listOf(oldDiscountRule))
        val dropshippingSale = dropshippingSaleOptional.get()
        val dropshippingSaleDTO = dropshippingSale.toDTO()

        val newDropshippingSaleDTO = dropshippingSaleDTO.copy()
        newDropshippingSaleDTO.specialConditions = newDiscountRulesJson

        val genericResponse = GenericResponse.SuccessResponse(newDropshippingSaleDTO)
        val serviceResponse = ServiceResponse.Success(newDropshippingSaleDTO)
        Mockito.`when`(dropshippingSaleService.findById(id)).thenReturn(dropshippingSaleOptional)
        Mockito.`when`(dropshippingSaleService.updateDropshippingSale(newDropshippingSaleDTO)).thenReturn(genericResponse)
        Mockito.`when`(discountRuleParser.parseDiscountRules(oldDiscountRulesJson)).thenReturn(mutableListOf(oldDiscountRule))
        Mockito.`when`(discountRuleParser.unparseDiscountRules(mutableListOf(newDiscountRule))).thenReturn(newDiscountRulesJson)

        val result = discountRuleService.updateDiscountRulesById(id, listOf(newDiscountRule))
        Assert.isTrue(result == serviceResponse, "Test 'Update discount rule - one previous rule - match' failed")
    }

    @Test
    fun `Update discount rule - one previous rule - no match`() {
        val discountQuantity1 = 5
        val discountCondition1 = DiscountCondition.EQUAL
        val discountTarget1 = DiscountTarget.TOTAL_PRICE
        val discountType1 = DiscountType.FIXED_PRICE
        val discountAmount1 = 10
        val discountRule1 = DiscountRule(discountQuantity1, discountCondition1, Discount(discountTarget1, discountType1, discountAmount1))
        val oldDiscountRulesJson = createDiscountRulesJson(listOf(discountRule1))

        val discountQuantity2 = 8
        val discountCondition2 = DiscountCondition.EQUAL
        val discountTarget2 = DiscountTarget.TOTAL_PRICE
        val discountType2 = DiscountType.FIXED_PRICE
        val discountAmount2 = 10000
        val discountRule2 = DiscountRule(discountQuantity2, discountCondition2, Discount(discountTarget2, discountType2, discountAmount2))

        val discountRules = ArrayList<DiscountRule>()
        discountRules.add(discountRule1)
        discountRules.add(discountRule2)
        val newDiscountRulesJson = createDiscountRulesJson(discountRules)

        val id = UUID.randomUUID()
        val dropshippingSaleOptional = createOptionalDropshippingSaleWithDiscounts(id, listOf(discountRule1))
        val dropshippingSale = dropshippingSaleOptional.get()
        val dropshippingSaleDTO = dropshippingSale.toDTO()

        val newDropshippingSaleDTO = dropshippingSaleDTO.copy()
        newDropshippingSaleDTO.specialConditions = newDiscountRulesJson

        val genericResponse = GenericResponse.SuccessResponse(newDropshippingSaleDTO)
        val serviceResponse = ServiceResponse.Success(newDropshippingSaleDTO)
        Mockito.`when`(dropshippingSaleService.findById(id)).thenReturn(dropshippingSaleOptional)
        Mockito.`when`(dropshippingSaleService.updateDropshippingSale(newDropshippingSaleDTO)).thenReturn(genericResponse)
        Mockito.`when`(discountRuleParser.parseDiscountRules(oldDiscountRulesJson)).thenReturn(mutableListOf(discountRule1))
        Mockito.`when`(discountRuleParser.unparseDiscountRules(discountRules)).thenReturn(newDiscountRulesJson)

        val result = discountRuleService.updateDiscountRulesById(id, listOf(discountRule2))
        Assert.isTrue(result == serviceResponse, "Test 'Update discount rule - one previous rule - no match' failed")
    }

    @Test
    fun `Update discount rule - two previous rules - one match`() {

        val discountQuantity1 = 5
        val discountCondition1 = DiscountCondition.EQUAL
        val discountTarget1 = DiscountTarget.TOTAL_PRICE
        val discountType1 = DiscountType.FIXED_PRICE
        val discountAmount1 = 10

        val discountQuantity2 = 8
        val discountCondition2 = DiscountCondition.EQUAL
        val discountTarget2 = DiscountTarget.TOTAL_PRICE
        val discountType2 = DiscountType.FIXED_PRICE
        val discountAmount2 = 10000

        val oldDiscountRule1 = DiscountRule(discountQuantity1, discountCondition1, Discount(discountTarget1, discountType1, discountAmount1))
        val discountRule2 = DiscountRule(discountQuantity2, discountCondition2, Discount(discountTarget2, discountType2, discountAmount2))

        val oldDiscountRules = ArrayList<DiscountRule>()
        oldDiscountRules.add(oldDiscountRule1)
        oldDiscountRules.add(discountRule2)
        val oldDiscountRulesJson = createDiscountRulesJson(oldDiscountRules)

        val newDiscountAmount = 100000
        val newDiscountRule1 = DiscountRule(discountQuantity1, discountCondition1, Discount(discountTarget1, discountType1, newDiscountAmount))
        val newDiscountRules = ArrayList<DiscountRule>()
        newDiscountRules.add(newDiscountRule1)
        newDiscountRules.add(discountRule2)
        val newDiscountRulesJson = createDiscountRulesJson(newDiscountRules)

        val id = UUID.randomUUID()
        val dropshippingSaleOptional = createOptionalDropshippingSaleWithDiscounts(id, oldDiscountRules)
        val dropshippingSale = dropshippingSaleOptional.get()
        val dropshippingSaleDTO = dropshippingSale.toDTO()

        val newDropshippingSaleDTO = dropshippingSaleDTO.copy()
        newDropshippingSaleDTO.specialConditions = newDiscountRulesJson

        val genericResponse = GenericResponse.SuccessResponse(newDropshippingSaleDTO)
        val serviceResponse = ServiceResponse.Success(newDropshippingSaleDTO)
        Mockito.`when`(dropshippingSaleService.findById(id)).thenReturn(dropshippingSaleOptional)
        Mockito.`when`(dropshippingSaleService.updateDropshippingSale(newDropshippingSaleDTO)).thenReturn(genericResponse)
        Mockito.`when`(discountRuleParser.parseDiscountRules(oldDiscountRulesJson)).thenReturn((oldDiscountRules))
        Mockito.`when`(discountRuleParser.unparseDiscountRules(newDiscountRules)).thenReturn(newDiscountRulesJson)

        val result = discountRuleService.updateDiscountRulesById(id, listOf(newDiscountRule1))
        Assert.isTrue(result == serviceResponse, "Test 'Update discount rule - two previous rules - one match' failed")
    }

    private fun createOptionalDropshippingSaleWithNoDiscount(id: UUID) =
        Optional.of(
            DropshippingSale(
                id = id,
                product = createProductTest(),
                merchant = createMerchantTest().toEntity(),
                amount = null,
                specialConditions = null,
                disabled = false,
                deletionDate = null
            )
        )

    private fun createOptionalDropshippingSaleWithDiscounts(id: UUID, discountRules: List<DiscountRule>): Optional<DropshippingSale> {
        return Optional.of(
            DropshippingSale(
                id = id,
                    product = createProductTest(),
                    merchant = createMerchantTest().toEntity(),
                amount = null,
                specialConditions = createDiscountRulesJson(discountRules),
                disabled = false,
                deletionDate = null
            )
        )
    }

    private fun createDiscountRulesJson(list: List<DiscountRule>): String {
        var json = "["
        for (discountRule: DiscountRule in list) {
            json = json +
                    "{\n" +
                    "  \"quantity\": \"${discountRule.quantity}\",\n" +
                    "  \"condition\": \"${discountRule.condition}\",\n" +
                    "  \"discount\": {\n" +
                    "    \"target\": \"${discountRule.discount.target}\",\n" +
                    "    \"type\": \"${discountRule.discount.type}\",\n" +
                    "    \"amount\": \"${discountRule.discount.amount}\"\n" +
                    "  }\n" +
                    "},"
        }
        json = "$json]"
        return json
    }
}

