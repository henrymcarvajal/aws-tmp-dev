package com.mps.payment.core.service.fundsdispersion

import com.mps.common.dto.ServiceResponse
import com.mps.payment.core.model.DeliveredOrder
import com.mps.payment.core.repository.MappedGuideNumberRepository
import com.mps.payment.core.service.OrderService
import com.mps.payment.core.TestScenarioType
import com.mps.payment.core.service.exceltools.ExcelService
import com.mps.payment.core.service.fundsdispersion.file.FundsDispersionFileService
import com.mps.payment.core.service.fundsdispersion.file.GeneratedDispersionFiles
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.springframework.mock.web.MockMultipartFile
import org.springframework.util.Assert
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream


internal class FundsDispersionServiceTest {

    @Mock
    private lateinit var deliveryExtractorService: DeliveryExtractorService

    @Mock
    private lateinit var mappedGuideNumberRepository: MappedGuideNumberRepository

    @Mock
    private lateinit var generalOrderService: OrderService

    @Mock
    private lateinit var excelService: ExcelService

    @Mock
    private lateinit var paymentCalculatorService: PaymentCalculatorService

    @Mock
    private lateinit var fundsDispersionFileService: FundsDispersionFileService

    @Mock
    private lateinit var fundsDispersionMailService: FundsDispersionMailService

    private lateinit var fundsDispersionService: FundsDispersionService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        fundsDispersionService = FundsDispersionService(
            deliveryExtractorService,
            mappedGuideNumberRepository,
            generalOrderService,
            excelService,
            paymentCalculatorService,
            fundsDispersionFileService,
            fundsDispersionMailService
        )
    }

    @Test
    fun `There is no excel`() {
        val testScenario = "There is no excel"
        val scenarioType = TestScenarioType.NEGATIVE
        val onTestFailureMessage = "$scenarioType test '$testScenario' failed"

        val serviceResponse = fundsDispersionService.generateDispersion(null)

        Assert.isTrue(serviceResponse is ServiceResponse.Error, onTestFailureMessage)
        Assert.isTrue(serviceResponse.objP as String == FundsDispersionService.EXCEL_FILE_NULL_ERR_MESSAGE, onTestFailureMessage)
    }

    @Test
    fun `Excel file is empty`() {
        val testScenario = "Excel file is empty"
        val scenarioType = TestScenarioType.NEGATIVE
        val onTestFailureMessage = "$scenarioType test '$testScenario' failed"

        val file = File("src/test/resources/excel/empty.xlsx")
        val input = FileInputStream(file)
        val multipartFile: MultipartFile = MockMultipartFile("empty", input)

        Mockito.`when`(deliveryExtractorService.extractDeliveries(multipartFile)).thenReturn(ServiceResponse.Error(DeliveryExtractorService.NOT_ALL_TAGS_LOADED_ERR_MESSAGE))

        val serviceResponse = fundsDispersionService.generateDispersion(multipartFile)

        Assert.isTrue(serviceResponse is ServiceResponse.Error, onTestFailureMessage)
        Assert.isTrue(serviceResponse.objP as String == DeliveryExtractorService.NOT_ALL_TAGS_LOADED_ERR_MESSAGE, onTestFailureMessage)
    }

    @Test
    fun `Excel file does not have all columns`() {
        val testScenario = "Excel file does not have all columns"
        val scenarioType = TestScenarioType.NEGATIVE
        val onTestFailureMessage = "$scenarioType test '$testScenario' failed"

        val file = File("src/test/resources/excel/not_all_columns.xlsx")
        val input = FileInputStream(file)
        val multipartFile: MultipartFile = MockMultipartFile("not_all_columns", input)

        Mockito.`when`(deliveryExtractorService.extractDeliveries(multipartFile)).thenReturn(ServiceResponse.Error(DeliveryExtractorService.NOT_ALL_TAGS_LOADED_ERR_MESSAGE))

        val serviceResponse = fundsDispersionService.generateDispersion(multipartFile)

        Assert.isTrue(serviceResponse is ServiceResponse.Error, onTestFailureMessage)
        Assert.isTrue(serviceResponse.objP as String == DeliveryExtractorService.NOT_ALL_TAGS_LOADED_ERR_MESSAGE, onTestFailureMessage)
    }

    @Test
    fun `Data with unexpected format`() {
        val testScenario = "Data with unexpected format"
        val scenarioType = TestScenarioType.NEGATIVE
        val onTestFailureMessage = "$scenarioType test '$testScenario' failed"

        val file = File("src/test/resources/excel/deliveries-standard_column_positions-unexpected_format.xlsx")
        val input = FileInputStream(file)
        val multipartFile: MultipartFile = MockMultipartFile("unexpected_format", input)

        Mockito.`when`(deliveryExtractorService.extractDeliveries(multipartFile)).thenReturn(ServiceResponse.Error(DeliveryExtractorService.UNRECOGNIZED_DATA_FORMAT_ERR_MESSAGE))

        val serviceResponse = fundsDispersionService.generateDispersion(multipartFile)

        Assert.isTrue(serviceResponse is ServiceResponse.Error, onTestFailureMessage)
        Assert.isTrue(serviceResponse.objP as String == DeliveryExtractorService.UNRECOGNIZED_DATA_FORMAT_ERR_MESSAGE, onTestFailureMessage)
    }

    @Test
    fun `There are no delivered orders in excel`() {
        val testScenario = "There are no delivered orders in excel"
        val scenarioType = TestScenarioType.POSITIVE
        val onTestFailureMessage = "$scenarioType test '$testScenario' failed"

        val file = File("src/test/resources/excel/no_deliveries-standard_column_positions.xlsx")
        val input = FileInputStream(file)
        val multipartFile: MultipartFile = MockMultipartFile("no_deliveries-standard_column_positions", input)

        Mockito.`when`(deliveryExtractorService.extractDeliveries(multipartFile)).thenReturn(ServiceResponse.Success(emptyList<DeliveredOrder>()))

        val generatedWorkbooks = GeneratedDispersionFiles(XSSFWorkbook(), emptyMap())
        Mockito.`when`(fundsDispersionFileService.generateDispersionWorkbooks(emptyMap())).thenReturn(generatedWorkbooks)

        val serviceResponse = fundsDispersionService.generateDispersion(multipartFile)

        Assert.isTrue(serviceResponse is ServiceResponse.Success, onTestFailureMessage)
        //Assert.isTrue(serviceResponse.objP as String == FundsDispersionService.EXCEL_NO_PROPER_FORMAT_ERR_MESSAGE, onTestFailureMessage)
    }

    @Test
    fun `Existing delivered orders in excel`() {
        val testScenario = "Existing delivered orders in excel"
        val scenarioType = TestScenarioType.POSITIVE
        val onTestFailureMessage = "$scenarioType test '$testScenario' failed"

        val file = File("src/test/resources/excel/deliveries-standard_column_positions.xlsx")
        val input = FileInputStream(file)
        val multipartFile: MultipartFile = MockMultipartFile("excel", input)

        Mockito.`when`(deliveryExtractorService.extractDeliveries(multipartFile)).thenReturn(ServiceResponse.Success(emptyList<DeliveredOrder>()))

        val generatedWorkbooks = GeneratedDispersionFiles(XSSFWorkbook(), emptyMap())
        Mockito.`when`(fundsDispersionFileService.generateDispersionWorkbooks(emptyMap())).thenReturn(generatedWorkbooks)

        val serviceResponse = fundsDispersionService.generateDispersion(multipartFile)

        Assert.isTrue(serviceResponse is ServiceResponse.Success, onTestFailureMessage)
        //Assert.isTrue(serviceResponse.objP as String == FundsDispersionService.EXCEL_NO_PROPER_FORMAT_ERR_MESSAGE, onTestFailureMessage)
    }
}