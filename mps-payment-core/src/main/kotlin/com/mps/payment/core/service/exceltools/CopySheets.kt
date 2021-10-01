package com.mps.payment.core.service.exceltools

import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * Taken from https://stackoverflow.com/questions/13090313/how-to-copy-a-sheet-between-excel-workbooks-in-java
 *
 * Adapted for Kotlin by Henry Carvajal
 */
internal class CellRangeAddressWrapper(theRange: CellRangeAddress) : Comparable<CellRangeAddressWrapper?> {
    var range: CellRangeAddress = theRange

    /**
     * @param other the object to compare.
     * @return -1 the current instance is prior to the object in parameter, 0: equal, 1: after...
     */
    override operator fun compareTo(other: CellRangeAddressWrapper?): Int {
        if (other != null) {
            return if (range.firstColumn < other.range.firstColumn
                && range.firstRow < other.range.firstRow
            ) {
                -1
            } else if (range.firstColumn == other.range.firstColumn
                && range.firstRow == other.range.firstRow
            ) {
                0
            } else {
                1
            }
        }
        throw NullPointerException("Other must not be null")
    }
}

class CopySheets {
    @Throws(IOException::class)
    fun mergeExcelFiles(book: Workbook, inList: List<InputStream?>): Workbook {
        for (fin in inList) {
            val b = WorkbookFactory.create(fin)
            for (i in 0 until b.numberOfSheets) {
                // not entering sheet name, because of duplicated names
                copySheets(book.createSheet(), b.getSheetAt(i))
            }
        }
        return book
    }

    companion object {
        /**
         * @param newSheet  the sheet to create from the copy.
         * @param sheet     the sheet to copy.
         * @param copyStyle true copy the style.
         */
        @JvmOverloads
        fun copySheets(newSheet: Sheet, sheet: Sheet, copyStyle: Boolean = true) {
            var maxColumnNum = 0
            val styleMap: MutableMap<Int?, CellStyle?>? = if (copyStyle) HashMap() else null
            for (i in sheet.firstRowNum..sheet.lastRowNum) {
                val srcRow = sheet.getRow(i)
                val destRow = newSheet.createRow(i)
                if (srcRow != null) {
                    copyRow(sheet, newSheet, srcRow, destRow, styleMap)
                    if (srcRow.lastCellNum > maxColumnNum) {
                        maxColumnNum = srcRow.lastCellNum.toInt()
                    }
                }
            }
            for (i in 0..maxColumnNum) {
                newSheet.setColumnWidth(i, sheet.getColumnWidth(i))
            }
        }

        /**
         * @param srcSheet  the sheet to copy.
         * @param destSheet the sheet to create.
         * @param srcRow    the row to copy.
         * @param destRow   the row to create.
         * @param styleMap  -
         */
        private fun copyRow(srcSheet: Sheet, destSheet: Sheet, srcRow: Row, destRow: Row, styleMap: MutableMap<Int?, CellStyle?>?) {
            // manage a list of merged zone in order to not insert two times a merged zone
            val mergedRegions: MutableSet<CellRangeAddressWrapper> = TreeSet()
            destRow.height = srcRow.height
            // reckoning delta rows
            val deltaRows = destRow.rowNum - srcRow.rowNum

            // pour chaque row
            for (j in srcRow.firstCellNum..srcRow.lastCellNum) {
                if (j >= 0) {
                    val oldCell = srcRow.getCell(j) // ancienne cell
                    var newCell = destRow.getCell(j) // new cell
                    if (oldCell != null) {
                        if (newCell == null) {
                            newCell = destRow.createCell(j)
                        }
                        // copy chaque cell
                        copyCell(oldCell, newCell, styleMap)
                        // copy les informations de fusion entre les cellules
                        //System.out.println("row num: " + srcRow.getRowNum() + " , col: " + (short)oldCell.getColumnIndex());
                        val mergedRegion = getMergedRegion(
                            srcSheet, srcRow.rowNum, oldCell.columnIndex
                                .toShort()
                        )
                        if (mergedRegion != null) {
                            //System.out.println("Selected merged region: " + mergedRegion.toString());
                            val newMergedRegion = CellRangeAddress(
                                mergedRegion.firstRow + deltaRows,
                                mergedRegion.lastRow + deltaRows,
                                mergedRegion.firstColumn,
                                mergedRegion.lastColumn
                            )
                            //System.out.println("New merged region: " + newMergedRegion.toString());
                            val wrapper = CellRangeAddressWrapper(newMergedRegion)
                            if (isNewMergedRegion(wrapper, mergedRegions)) {
                                mergedRegions.add(wrapper)
                                destSheet.addMergedRegion(wrapper.range)
                            }
                        }
                    }
                }
            }
        }

        /**
         * @param oldCell
         * @param newCell
         * @param styleMap
         */
        private fun copyCell(oldCell: Cell, newCell: Cell?, styleMap: MutableMap<Int?, CellStyle?>?) {
            if (styleMap != null) {
                if (oldCell.sheet.workbook === newCell!!.sheet.workbook) {
                    newCell!!.cellStyle = oldCell.cellStyle
                } else {
                    val stHashCode = oldCell.cellStyle.hashCode()
                    var newCellStyle = styleMap[stHashCode]
                    if (newCellStyle == null) {
                        newCellStyle = newCell!!.sheet.workbook.createCellStyle()
                        newCellStyle.cloneStyleFrom(oldCell.cellStyle)
                        styleMap[stHashCode] = newCellStyle
                    }
                    newCell!!.cellStyle = newCellStyle
                }
            }
            when (oldCell.cellType) {
                CellType.STRING -> newCell!!.setCellValue(oldCell.stringCellValue)
                CellType.NUMERIC -> newCell!!.setCellValue(oldCell.numericCellValue)
                CellType.BOOLEAN -> newCell!!.setCellValue(oldCell.booleanCellValue)
                CellType.ERROR -> newCell!!.setCellErrorValue(oldCell.errorCellValue)
                CellType.FORMULA -> newCell!!.cellFormula = oldCell.cellFormula
                else -> {
                }
            }
        }

        /**
         * Récupère les informations de fusion des cellules dans la sheet source pour les appliquer
         * à la sheet destination...
         * Récupère toutes les zones merged dans la sheet source et regarde pour chacune d'elle si
         * elle se trouve dans la current row que nous traitons.
         * Si oui, retourne l'objet CellRangeAddress.
         *
         * @param sheet   the sheet containing the data.
         * @param rowNum  the num of the row to copy.
         * @param cellNum the num of the cell to copy.
         * @return the CellRangeAddress created.
         */
        private fun getMergedRegion(sheet: Sheet, rowNum: Int, cellNum: Short): CellRangeAddress? {
            for (i in 0 until sheet.numMergedRegions) {
                val merged = sheet.getMergedRegion(i)
                if (merged.isInRange(rowNum, cellNum.toInt())) {
                    return merged
                }
            }
            return null
        }

        /**
         * Check that the merged region has been created in the destination sheet.
         *
         * @param newMergedRegion the merged region to copy or not in the destination sheet.
         * @param mergedRegions   the list containing all the merged region.
         * @return true if the merged region is already in the list or not.
         */
        private fun isNewMergedRegion(newMergedRegion: CellRangeAddressWrapper, mergedRegions: Set<CellRangeAddressWrapper>): Boolean {
            return !mergedRegions.contains(newMergedRegion)
        }
    }
}