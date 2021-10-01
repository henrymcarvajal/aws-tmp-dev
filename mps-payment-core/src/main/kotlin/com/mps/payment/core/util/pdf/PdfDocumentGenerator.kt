package com.mps.payment.core.util.pdf

import com.lowagie.text.*
import com.lowagie.text.pdf.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime

const val TITLE_1="Guia"
const val TITLE_2="Nombre Cliente"
const val TITLE_3="Célular Cliente"
@Service
class PdfDocumentGenerator {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun getPdf(data: List<FreightData>): ByteArrayInputStream? {
        return try {
            val out = ByteArrayOutputStream()
            val document = Document(PageSize.LETTER)
            val headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD)
            val para  = Paragraph("Fecha de despacho:${LocalDate.now()}",headFont)
            PdfWriter.getInstance(document, out)
            document.open()
            document.add(para)
            if (data.isEmpty()) {
                document.add(Chunk("No hay rotulos pendientes"))
            }
            val table = createTable(listOf(TITLE_1, TITLE_2, TITLE_3),data)
            document.add(table)
            document.close()
            ByteArrayInputStream(out.toByteArray())
        } catch (ex: Exception) {
            logger.error("Error generating pdf", ex)
            null
        }
    }

    private fun createTable(headers:List<String>,data: List<FreightData>): PdfPTable {
        val table = PdfPTable(3)
        table.widthPercentage = 98f
        table.setWidths(intArrayOf(3, 8,5))
        val headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD)

        headers.forEach {
            val hcell = PdfPCell(Phrase(it, headFont))
            hcell.horizontalAlignment = Element.ALIGN_LEFT
            table.addCell(hcell)
        }
        data.forEach {
            val guideCell = PdfPCell(Phrase(it.guide))
            guideCell.verticalAlignment = Element.ALIGN_MIDDLE
            guideCell.horizontalAlignment = Element.ALIGN_LEFT
            table.addCell(guideCell)

            val customerNameCell = PdfPCell(Phrase(it.customerName))
            customerNameCell.verticalAlignment = Element.ALIGN_MIDDLE
            customerNameCell.horizontalAlignment = Element.ALIGN_LEFT
            table.addCell(customerNameCell)

            val customerContactNumberCell = PdfPCell(Phrase(it.customerContactNumber))
            customerContactNumberCell.verticalAlignment = Element.ALIGN_MIDDLE
            customerContactNumberCell.horizontalAlignment = Element.ALIGN_LEFT
            table.addCell(customerContactNumberCell)
        }
        table.setSpacingAfter(10f)
        table.setSpacingBefore(10f)

        table.horizontalAlignment = Element.ALIGN_LEFT

        return table
    }
}

data class FreightData(
        val guide:String,
        val customerName:String,
        val customerContactNumber:String
)