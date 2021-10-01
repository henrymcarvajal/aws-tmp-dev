package com.mps.payment.core.service.fundsdispersion.file

import com.mps.payment.core.model.DeliveredOrder
import com.mps.payment.core.model.MerchantInfo
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service


@Service
class DetailSheetService {

    private companion object {

        val DETAIL_HEADER_COLOR = IndexedColors.AQUA

        val DETAILED_HEADER_VALUES = arrayListOf(
            "Número de guía",
            "Nombre Producto",
            "Monto",
            "Valor a pagar vendedor"
        )
    }

    fun createDetailedSheets(consolidatedDeliveries: Map<MerchantInfo, List<DeliveredOrder>>, workbook: Workbook) : Map<List<String>, Sheet> {

        val detailSheet = HashMap<List<String>, Sheet>()
        val headerStyle = workbook.createCellStyle()
        headerStyle.fillForegroundColor = DETAIL_HEADER_COLOR.getIndex()
        headerStyle.fillPattern = FillPatternType.SOLID_FOREGROUND

        val font = (workbook as XSSFWorkbook).createFont()
        font.fontName = FundsDispersionFileConstants.XL_FONT_NAME
        font.fontHeightInPoints = 11.toShort()
        font.bold = true
        headerStyle.setFont(font)

        val currencyStyle: CellStyle = workbook.createCellStyle()
        currencyStyle.dataFormat = FundsDispersionFileConstants.CURRENCY_CELL_FORMAT.toShort()

        consolidatedDeliveries.forEach { (merchantInfo, deliveries) ->
            val sheet = workbook.createSheet("${merchantInfo.NIT}-${merchantInfo.name}-${merchantInfo.type}")
            addDetailHeader(deliveries[0], sheet, headerStyle)
            val lastIndex = addDetailBody(deliveries, sheet, currencyStyle)
            addDetailFooter(merchantInfo, sheet, currencyStyle, lastIndex)
            detailSheet[listOf(merchantInfo.name, merchantInfo.email)] = sheet
        }

        return detailSheet
    }

    private fun getColumnWidth(header: String): Int {
        return header.length * 300
    }

    private fun getCharForNumber(i: Int): String? {
        return if (i in 1..26) {
            ((i + 'A'.toInt() - 1).toChar()).toString()
        } else null
    }

    private fun addDetailHeader(deliveredOrder: DeliveredOrder, sheet: Sheet, headerStyle: CellStyle) {
        val headerRow = sheet.createRow(0)

        var cellNumber = 0
        var cell = headerRow.createCell(cellNumber++)
        var headerValue = DETAILED_HEADER_VALUES[0]
        cell.setCellValue(headerValue)
        cell.cellStyle = headerStyle
        sheet.setColumnWidth(cellNumber, getColumnWidth(headerValue))
        cell = headerRow.createCell(cellNumber++)
        headerValue = DETAILED_HEADER_VALUES[1]
        cell.setCellValue(headerValue)
        cell.cellStyle = headerStyle
        sheet.setColumnWidth(cellNumber, getColumnWidth(headerValue))
        cell = headerRow.createCell(cellNumber++)
        headerValue = DETAILED_HEADER_VALUES[2]
        cell.setCellValue(headerValue)
        cell.cellStyle = headerStyle
        sheet.setColumnWidth(cellNumber, getColumnWidth(headerValue))

        deliveredOrder.applicableCharges.keys.forEach {
            cell = headerRow.createCell(cellNumber++)
            cell.setCellValue(it)
            cell.cellStyle = headerStyle
            sheet.setColumnWidth(cellNumber, getColumnWidth(it))
        }

        /*cell = headerRow.createCell(cellNumber++)
        cell.setCellValue("Total cargos")*/
        cell = headerRow.createCell(cellNumber++)
        headerValue = DETAILED_HEADER_VALUES[3]
        cell.cellStyle = headerStyle
        cell.setCellValue(headerValue)
        cell.cellStyle = headerStyle
        sheet.setColumnWidth(cellNumber, getColumnWidth(headerValue))
    }

    private fun addDetailBody(deliveredOrders: List<DeliveredOrder>, sheet: Sheet, currencyStyle: CellStyle): Int {
        val headerRowIndex = 1

        for ((i, deliveredOrder: DeliveredOrder) in deliveredOrders.withIndex()) {
            val deliveryRow = sheet.createRow(headerRowIndex + i)

            var cellNumber = 0
            var cell = deliveryRow.createCell(cellNumber++)
            cell.setCellValue(deliveredOrder.guideNumber.toDouble())
            cell = deliveryRow.createCell(cellNumber++)
            cell.setCellValue(deliveredOrder.productName)
            cell = deliveryRow.createCell(cellNumber++)
            deliveredOrder.collectedAmount?.let {
                run {
                    cell.cellStyle = currencyStyle
                    cell.setCellValue(it.toDouble())
                }
            }

            deliveredOrder.applicableCharges.keys.forEach {
                cell = deliveryRow.createCell(cellNumber++)

                deliveredOrder.applicableCharges[it]?.let { it1 ->
                    run {
                        cell.setCellValue(it1.toDouble())
                        cell.cellStyle = currencyStyle
                    }
                }
            }

            /*cell = deliveryRow.createCell(cellNumber++)
            cell.setCellValue(delivery.totalCharges.toDouble())
            cell.cellStyle = currencyStyle*/
            cell = deliveryRow.createCell(cellNumber++)
            cell.setCellValue(deliveredOrder.totalPayment.toDouble())
            cell.cellStyle = currencyStyle
        }

        val lastBodyIndex = headerRowIndex + deliveredOrders.size

        return addTotalsRow(deliveredOrders[0].applicableCharges, lastBodyIndex, sheet, currencyStyle)
    }

    private fun addTotalsRow(applicableCharges: Map<String, Int>, lastBodyIndex: Int, sheet: Sheet, currencyStyle: CellStyle): Int {
        val totalsIndex = lastBodyIndex + 1
        val totalsRow = sheet.createRow(totalsIndex)
        var cell = totalsRow.createCell(0)
        cell.setCellValue("Totales")


        cell = totalsRow.createCell(2)
        var columnName = getCharForNumber(2 + 1)
        cell.cellFormula = "SUM(${columnName}2:${columnName}$lastBodyIndex)"
        cell.cellStyle = currencyStyle

        val totalsStartingColumn = 3

        for ((i, _: String) in applicableCharges.keys.withIndex()) {
            cell = totalsRow.createCell(totalsStartingColumn + i)
            columnName = getCharForNumber(totalsStartingColumn + i + 1)
            cell.cellFormula = "SUM(${columnName}2:${columnName}$lastBodyIndex)"
            cell.cellStyle = currencyStyle
        }

        cell = totalsRow.createCell(totalsStartingColumn + applicableCharges.keys.size)
        columnName = getCharForNumber(totalsStartingColumn + applicableCharges.keys.size + 1)
        cell.cellFormula = "SUM(${columnName}2:${columnName}$lastBodyIndex)"
        cell.cellStyle = currencyStyle

        return totalsIndex + 1
    }

    private fun addDetailFooter(merchantInfo: MerchantInfo, sheet: Sheet, currencyStyle: CellStyle, lastIndex: Int) {
        var rowNumber = lastIndex + 1

        val totalRow = sheet.createRow(rowNumber++)

        var cell = totalRow.createCell(0)
        cell.setCellValue("Descuentos")

        merchantInfo.applicableCharges.keys.forEach {
            val chargesRow = sheet.createRow(rowNumber++)

            cell = chargesRow.createCell(0)
            cell.setCellValue(it)
            cell = chargesRow.createCell(1)
            merchantInfo.applicableCharges[it]?.let { it1 -> cell.setCellValue(it1.toDouble()) }
            cell.cellStyle = currencyStyle
        }

        val totalsDiscountsRow = sheet.createRow(rowNumber++)
        sheet.createRow(rowNumber++)

        cell = totalsDiscountsRow.createCell(0)
        cell.setCellValue("Total descuentos")
        cell = totalsDiscountsRow.createCell(1)
        cell.setCellValue(merchantInfo.chargesTotal.toDouble())
        cell.cellStyle = currencyStyle

        val totalPaymentRow = sheet.createRow(rowNumber++)
        sheet.createRow(rowNumber++)
        cell = totalPaymentRow.createCell(0)
        cell.setCellValue("Total a pagar")
        cell = totalPaymentRow.createCell(1)
        cell.setCellValue(merchantInfo.paymentTotal.toDouble())
        cell.cellStyle = currencyStyle
    }
}

