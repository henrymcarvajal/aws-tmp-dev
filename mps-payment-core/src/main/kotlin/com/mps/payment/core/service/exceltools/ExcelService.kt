package com.mps.payment.core.service.exceltools

import com.mps.common.util.img.convertMultiPartToFile
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Service
class ExcelService {

    fun readWorkbook(multipartFile: MultipartFile): Workbook {
        val file = convertMultiPartToFile(multipartFile)
        return XSSFWorkbook(file)
    }

    fun writeWorkbook(workbook: Workbook): ByteArrayInputStream {
        val out = ByteArrayOutputStream()
        workbook.write(out)
        return ByteArrayInputStream(out.toByteArray())
    }

    fun copySheets(newSheet: Sheet, sheet: Sheet, copyStyle: Boolean = true) {
        CopySheets.copySheets(newSheet, sheet, copyStyle)
    }

    fun getCellValue(cell: Cell, expectedType: CellType): Any? {
        return when (cell.cellType) {
            CellType.NUMERIC -> return cell.numericCellValue
            CellType.STRING -> return cell.stringCellValue
            CellType.BOOLEAN -> return cell.booleanCellValue
            CellType.BLANK -> {
                return when (expectedType) {
                    CellType.NUMERIC -> 0.0
                    CellType.STRING -> ""
                    else -> ""
                }
            }
            else -> ""
        }
    }
}