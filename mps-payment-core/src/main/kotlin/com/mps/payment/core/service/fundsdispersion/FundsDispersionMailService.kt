package com.mps.payment.core.service.fundsdispersion

import com.mps.payment.core.email.EmailSender
import com.mps.payment.core.service.CONST_MESSAGE
import org.apache.poi.ss.usermodel.Workbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Service
class FundsDispersionMailService(
    private val emailSender: EmailSender
) {
    private companion object {
        const val TEMPLATE_EMAIL_PLAIN_TEXT = "plane_template_text"
        const val ORDER_CUT_MAILED = "Corte Ordenes - MPS"
    }

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun sendFundsDispersionEmails(merchantWorkbooks: Map<List<String>, Workbook>) {
        val filesToClean = mutableListOf<String>()
        merchantWorkbooks.forEach { (keyInfo, workbook) ->
            val excelName = getFilename(keyInfo[0])
            val attachmentFile = File(excelName)
            saveWorkbookToDisk(excelName, workbook)
            emailSender.sendEmailWithTemplateAndAttachments(
                recipient = keyInfo[1], templateName = TEMPLATE_EMAIL_PLAIN_TEXT, title = ORDER_CUT_MAILED,
                data = mutableMapOf(
                    CONST_MESSAGE to "A continuación, encuentra el corte de MiPagoSeguro a la fecha"
                ), attachments = mapOf(excelName to attachmentFile)
            )
            filesToClean.add(excelName)
        }

        cleanUpFiles(filesToClean)
    }

    private fun getFilename(merchantName: String): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        return "corte-$merchantName-${LocalDateTime.now().format(formatter)}.xlsx"
    }

    private fun saveWorkbookToDisk(excelName: String, workbook: Workbook) {
        try {
            val diskFile = FileOutputStream(excelName)
            workbook.write(diskFile)
            diskFile.close()
        } catch (e: Exception) {
            log.error("Error saving workbook to disk", e)
        }
    }

    private fun cleanUpFiles(filesToClean: List<String>) {
        filesToClean.forEach {
            try {
                Files.delete(Path.of(it))
            } catch (e: Exception) {
                log.error("Error cleaning up files", e)
            }
        }
    }
}


