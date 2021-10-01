package com.mps.payment.core.service.fundsdispersion.file

import com.mps.payment.core.model.DeliveredOrder
import com.mps.payment.core.model.MerchantInfo
import com.mps.payment.core.service.exceltools.ExcelService
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service

@Service
class FundsDispersionFileService(
    private val summarySheetService: SummarySheetService,
    private val detailSheetService: DetailSheetService,
    private val excelService: ExcelService
) {

    fun generateDispersionWorkbooks(consolidatedDeliveries: Map<MerchantInfo, List<DeliveredOrder>>): GeneratedDispersionFiles {
        val summaryWorkbook = XSSFWorkbook()
        createSummarySheet(consolidatedDeliveries.keys, summaryWorkbook)

        val workbooks = HashMap<List<String>, Workbook>()
        if (consolidatedDeliveries.isNotEmpty()) {
            val sheets = createDetailedSheets(consolidatedDeliveries, summaryWorkbook)
            sheets.forEach { (t, u) -> workbooks[t] = createDetailWorkbooks(t, u) }
        }

        return GeneratedDispersionFiles(summaryWorkbook, workbooks)
    }

    private fun createSummarySheet(merchantsInfo: Set<MerchantInfo>, workbook: Workbook) {
        summarySheetService.createSummarySheet(merchantsInfo, workbook)
    }

    private fun createDetailedSheets(consolidatedDeliveries: Map<MerchantInfo, List<DeliveredOrder>>, workbook: Workbook): Map<List<String>, Sheet> {
        return detailSheetService.createDetailedSheets(consolidatedDeliveries, workbook)
    }

    private fun createDetailWorkbooks(keys: List<String>, sheet: Sheet): Workbook {
        val summaryWorkbook = XSSFWorkbook()
        val detailSheet = summaryWorkbook.createSheet(keys[0])
        excelService.copySheets(detailSheet, sheet)
        return summaryWorkbook
    }
}

