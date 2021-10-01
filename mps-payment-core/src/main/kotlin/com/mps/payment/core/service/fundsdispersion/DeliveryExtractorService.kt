package com.mps.payment.core.service.fundsdispersion

import com.mps.common.dto.ServiceResponse
import com.mps.payment.core.model.DeliveredOrder
import com.mps.payment.core.service.exceltools.ExcelService
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile


@Service
class DeliveryExtractorService(
    private val excelService: ExcelService,
) {

    companion object {
        private const val GUIDE_NUMBER_TAG = "Nº Guía Entrégalo"
        private const val DELIVERY_STATE_TAG = "Estado"
        private const val COLLECTED_AMOUNT_TAG = "Recaudo"
        private const val FREIGHT_TOTAL_COST_TAG = "Costo Total Servicio"

        val COLUMN_TAGS = mutableListOf(GUIDE_NUMBER_TAG, DELIVERY_STATE_TAG, COLLECTED_AMOUNT_TAG, FREIGHT_TOTAL_COST_TAG)

        const val NOT_ALL_TAGS_LOADED_ERR_MESSAGE = "Not all tags could be loaded"
        const val UNRECOGNIZED_DATA_FORMAT_ERR_MESSAGE = "Unrecognized format loading data"
    }

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun extractDeliveries(multipartFile: MultipartFile): ServiceResponse<*> {
        val workbook = excelService.readWorkbook(multipartFile)
        val sheet = workbook.getSheetAt(0)
        val indexes = findIndexes(sheet)
        return if (indexes != null) {
            if (indexes.isNotEmpty()) {
                val deliveries = loadDeliveries(indexes, sheet)
                if (deliveries != null) {
                    ServiceResponse.Success(deliveries)
                } else {
                    ServiceResponse.Error(UNRECOGNIZED_DATA_FORMAT_ERR_MESSAGE)
                }
            } else {
                ServiceResponse.Error(NOT_ALL_TAGS_LOADED_ERR_MESSAGE)
            }
        } else {
            ServiceResponse.Error(NOT_ALL_TAGS_LOADED_ERR_MESSAGE)
        }
    }

    private fun loadDeliveries(indexes: Map<String, Int>, mainSheet: Sheet): List<DeliveredOrder>? {
        val list = ArrayList<DeliveredOrder>()

        try {
            for ((i, row: Row) in mainSheet.withIndex()) {
                if (i != 0) {
                    var guideNumber = Int.MIN_VALUE
                    var deliveryState = ""
                    var collectedCash = 0
                    var freightTotalCost = 0

                    var cell = row.getCell(indexes[GUIDE_NUMBER_TAG]!!)
                    if (cell != null) {
                        guideNumber = (excelService.getCellValue(cell, CellType.NUMERIC) as Double).toInt()
                    }

                    cell = row.getCell(indexes[DELIVERY_STATE_TAG]!!)
                    if (cell != null) {
                        deliveryState = excelService.getCellValue(cell, CellType.STRING) as String
                    }

                    cell = row.getCell(indexes[COLLECTED_AMOUNT_TAG]!!)
                    if (cell != null) {
                        collectedCash = (excelService.getCellValue(cell, CellType.NUMERIC) as Double).toInt()
                    }

                    cell = row.getCell(indexes[FREIGHT_TOTAL_COST_TAG]!!)
                    if (cell != null) {
                        freightTotalCost = (excelService.getCellValue(cell, CellType.NUMERIC) as Double).toInt()
                    }

                    if (guideNumber != Int.MIN_VALUE && deliveryState != "" && collectedCash != Int.MIN_VALUE && freightTotalCost != Int.MIN_VALUE) {
                        list.add(DeliveredOrder(guideNumber, deliveryState, collectedCash, freightTotalCost))
                    }
                }
            }
        } catch (e: Exception) {
            log.error("Error loading deliveries", e)
            return null
        }

        return list
    }

    private fun findIndexes(sheet: Sheet): Map<String, Int>? {
        val indexes = mutableMapOf<String, Int>()
        val headerRow = sheet.getRow(0) ?: return emptyMap()
        var headersFound = 0
        COLUMN_TAGS.forEach {
            for (i in headerRow.firstCellNum..headerRow.lastCellNum) {
                val cell = headerRow.getCell(i)
                if (cell != null) {
                    if (it == cell.stringCellValue) {
                        indexes[it] = i
                        headersFound++
                        break
                    }
                }
            }
        }
        if (headersFound != COLUMN_TAGS.size) {
            return null
        }
        return indexes
    }
}