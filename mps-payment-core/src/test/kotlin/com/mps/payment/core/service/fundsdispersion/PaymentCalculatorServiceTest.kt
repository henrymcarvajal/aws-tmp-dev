package com.mps.payment.core.service.fundsdispersion

import com.mps.payment.core.TestScenarioType
import com.mps.payment.core.model.DeliveredOrder
import com.mps.payment.core.model.MerchantInfo
import com.mps.payment.core.service.exceltools.ExcelService
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.util.Assert

internal class PaymentCalculatorServiceTest {

    @Mock
    private lateinit var excelService: ExcelService

    private lateinit var paymentCalculatorService: PaymentCalculatorService

    @BeforeEach
    fun setup() {
        paymentCalculatorService = PaymentCalculatorService()
    }

    @Test
    fun calculatePayments() {
        val testScenario = "Excel file is empty"
        val scenarioType = TestScenarioType.NEGATIVE
        val onTestFailureMessage = "$scenarioType test '$testScenario' failed"

        val consolidatedMerchants : Map<MerchantInfo, MutableList<DeliveredOrder>> = emptyMap()
        val unModifiedConsolidatedMerchants = consolidatedMerchants
        paymentCalculatorService.calculatePayments(consolidatedMerchants)

        Assert.isTrue(consolidatedMerchants.equals(unModifiedConsolidatedMerchants), onTestFailureMessage)
    }
}