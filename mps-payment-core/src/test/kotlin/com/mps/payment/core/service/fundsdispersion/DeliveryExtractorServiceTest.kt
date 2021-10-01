package com.mps.payment.core.service.fundsdispersion

import com.mps.common.dto.ServiceResponse
import com.mps.payment.core.TestScenarioType
import com.mps.payment.core.service.exceltools.ExcelService
import org.apache.poi.ss.usermodel.Workbook
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

internal class DeliveryExtractorServiceTest {

    @Mock
    private lateinit var excelService: ExcelService

    private lateinit var deliveryExtractorService: DeliveryExtractorService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        deliveryExtractorService = DeliveryExtractorService(excelService)
    }

    @Test
    fun `Excel file is empty`() {
        val testScenario = "Excel file is empty"
        val scenarioType = TestScenarioType.NEGATIVE
        val onTestFailureMessage = "$scenarioType test '$testScenario' failed"

        val file = File("src/test/resources/excel/empty.xlsx")
        val input = FileInputStream(file)
        val multipartFile: MultipartFile = MockMultipartFile("empty", input)

        val workbook = XSSFWorkbook()
        workbook.createSheet()
        Mockito.`when`(excelService.readWorkbook(multipartFile)).thenReturn(workbook)

        val serviceResponse = deliveryExtractorService.extractDeliveries(multipartFile)

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

        Mockito.`when`(excelService.readWorkbook(multipartFile)).thenReturn(createWorkbookWithEmptySheet())

        val serviceResponse = deliveryExtractorService.extractDeliveries(multipartFile)

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

        Mockito.`when`(excelService.readWorkbook(multipartFile)).thenReturn(createWorkbookWithFaultyData(file))

        val serviceResponse = deliveryExtractorService.extractDeliveries(multipartFile)

        Assert.isTrue(serviceResponse is ServiceResponse.Error, onTestFailureMessage)
        Assert.isTrue(serviceResponse.objP as String == DeliveryExtractorService.UNRECOGNIZED_DATA_FORMAT_ERR_MESSAGE, onTestFailureMessage)
    }

    private fun createWorkbookWithEmptySheet() : Workbook {
        val workbook = XSSFWorkbook()
        workbook.createSheet()
        return workbook
    }

    private fun createWorkbookWithFaultyData(file : File) : Workbook {
        val workbook = XSSFWorkbook()
        workbook.createSheet()
        //return workbook

        return XSSFWorkbook(file)
    }
}