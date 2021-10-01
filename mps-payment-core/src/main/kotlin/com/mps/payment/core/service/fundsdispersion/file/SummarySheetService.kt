package com.mps.payment.core.service.fundsdispersion.file

import com.mps.payment.core.model.MerchantInfo
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Service
class SummarySheetService {

    private companion object {

        val SUMMARY_HEADER_COLOR = IndexedColors.LIGHT_GREEN

        val SUMMARY_HEADER_VALUES = arrayListOf(
            "Nombre de comercio",
            "Nit de comercio",
            "Número de cuenta",
            "Tipo de cuenta",
            "Banco de cuenta",
            "Total a consignar"
        )
    }

    fun createSummarySheet(merchantInfos: Set<MerchantInfo>, workbook: Workbook) {
        val headerStyle: CellStyle = workbook.createCellStyle()
        headerStyle.fillForegroundColor = SUMMARY_HEADER_COLOR.getIndex()
        headerStyle.fillPattern = FillPatternType.SOLID_FOREGROUND

        val font = (workbook as XSSFWorkbook).createFont()
        font.fontName = FundsDispersionFileConstants.XL_FONT_NAME
        font.fontHeightInPoints = 11.toShort()
        font.bold = true
        headerStyle.setFont(font)

        val currencyStyle: CellStyle = workbook.createCellStyle()
        currencyStyle.dataFormat = FundsDispersionFileConstants.CURRENCY_CELL_FORMAT.toShort()

        val sheet = workbook.createSheet(getSheetName())
        addSummaryHeader(sheet, headerStyle)
        if (merchantInfos.isNotEmpty()) {
            addSummaryBody(merchantInfos, sheet, currencyStyle)
            addSummaryFooter(merchantInfos.size, sheet, currencyStyle)
        }
    }

    private fun getSheetName(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        return "Dispersion-${LocalDateTime.now().format(formatter)}"
    }

    private fun getColumnWidth(header: String): Int {
        return header.length * 300
    }

    private fun addSummaryHeader(sheet: Sheet, headerStyle: CellStyle) {
        val header: Row = sheet.createRow(0)

        for ((i, headerValue: String) in SUMMARY_HEADER_VALUES.withIndex()) {
            val headerCell: Cell = header.createCell(i)
            headerCell.setCellValue(headerValue)
            headerCell.cellStyle = headerStyle
            sheet.setColumnWidth(i, getColumnWidth(headerValue))
        }
    }

    private fun addSummaryBody(merchantInfos: Set<MerchantInfo>, sheet: Sheet, currencyStyle: CellStyle) {
        for ((i, merchantInfo: MerchantInfo) in merchantInfos.withIndex()) {
            val deliveryRow = sheet.createRow(i + 1)

            var cellNumber = 0
            var cell: Cell = deliveryRow.createCell(cellNumber++)
            cell.setCellValue(merchantInfo.name)
            cell = deliveryRow.createCell(cellNumber++)
            cell.setCellValue(merchantInfo.NIT)
            cell = deliveryRow.createCell(cellNumber++)
            cell.setCellValue(merchantInfo.accountInfo.number)
            cell = deliveryRow.createCell(cellNumber++)
            cell.setCellValue(merchantInfo.accountInfo.type)
            cell = deliveryRow.createCell(cellNumber++)
            cell.setCellValue(merchantInfo.accountInfo.bankName)
            cell = deliveryRow.createCell(cellNumber)
            cell.setCellValue(merchantInfo.paymentTotal.toDouble())
            cell.cellStyle = currencyStyle
        }
    }

    private fun addSummaryFooter(summaryRows: Int, sheet: Sheet, currencyStyle: CellStyle) {
        val totalsRowIndex = summaryRows + 1
        val totalsRow = sheet.createRow(totalsRowIndex)
        var cell = totalsRow.createCell(0)
        cell.setCellValue("Total")

        cell = totalsRow.createCell(5)
        cell.cellFormula = "SUM(F2:F$totalsRowIndex)"
        cell.cellStyle = currencyStyle
    }
}

