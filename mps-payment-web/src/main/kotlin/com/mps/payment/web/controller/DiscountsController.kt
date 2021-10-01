package com.mps.payment.web.controller

import com.mps.common.dto.ServiceResponse
import com.mps.payment.core.model.DiscountRule
import com.mps.payment.core.service.DiscountRuleService
import com.mps.payment.core.util.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid

@RestController
@RequestMapping(path = ["discounts"])
class DiscountsController(
    private val discountRuleService: DiscountRuleService
) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/{dropshippingSaleId}")
    fun getDiscounts(@PathVariable dropshippingSaleId: UUID): ResponseEntity<*> {
        return try {
            when (val serviceResponse = discountRuleService.findDiscountRulesById(dropshippingSaleId)) {
                is ServiceResponse.Success -> ResponseEntity.ok().body(serviceResponse.objP as List<*>)
                is ServiceResponse.Error -> ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            log.error("Error getting discounts", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error getting discounts: ${ExceptionUtils.toString(e)}")
        }
    }

    @PatchMapping
    fun patchDiscounts(@Valid @RequestBody updateDiscountRulesRequest: UpdateDiscountRulesRequest): ResponseEntity<*> {
        return try {
            when (val serviceResponse =
                discountRuleService.updateDiscountRulesById(updateDiscountRulesRequest.dropshippingSaleId, updateDiscountRulesRequest.discountRules)) {
                is ServiceResponse.Success -> ResponseEntity.ok(serviceResponse.obj as Any)
                is ServiceResponse.Error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(serviceResponse.message)
            }
        } catch (e: Exception) {
            log.error("Error updating discounts", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error updating discounts: ${ExceptionUtils.toString(e)}")
        }
    }
}

data class UpdateDiscountRulesRequest(
    val dropshippingSaleId: UUID,
    @get:Valid val discountRules: List<@Valid DiscountRule>
)