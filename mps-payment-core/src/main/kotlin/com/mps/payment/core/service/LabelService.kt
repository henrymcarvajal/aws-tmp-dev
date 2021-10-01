
package com.mps.payment.core.service

import com.mps.payment.core.model.GeneralOrderDrop
import com.mps.payment.core.repository.OrderRepository
import com.mps.payment.core.util.pdf.FreightData
import com.mps.payment.core.util.pdf.PdfDocumentGenerator
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.*
import java.net.URL
import java.util.*


@Service
class LabelService(
        private val orderRepository: OrderRepository,
        private val pdfDocumentGenerator:PdfDocumentGenerator,
        private val customerService: CustomerService
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun getPdf(id: UUID):  ByteArrayInputStream? =  buildingPDFsForOrders(orderRepository.getLabelOrdersWithoutLabelPerMerchant(id))

    fun getPdf(ids: Array<UUID>) = buildingPDFsForOrders(orderRepository.findByIdIn(ids.toList()))

    private fun buildingPDFsForOrders(orders: List<GeneralOrderDrop>) =
            try {
                val consolidatePdf = generateConsolidatePDF(orders)
                val bytes = mergePdfs(orders.map { it.label },consolidatePdf)
                if (bytes != null) {
                    orders.forEach {
                        if(it.label!=null && it.label.isNotBlank() && it.guideNumber!=null){
                            it.isLabeled = true
                        }
                    }
                    orderRepository.saveAll(orders)
                }
                bytes
            } catch (e: Exception) {
                logger.error("Error generating pdf", e)
                null
            }

    private fun generateConsolidatePDF(orderList: List<GeneralOrderDrop>): ByteArrayInputStream? {
        val data = orderList.map {
            val customerOption = customerService.getCustomerById(it.customerId)
            val customeValue = if (customerOption.isEmpty) {
                null
            } else {
                customerOption.get()
            }
            FreightData(guide = it.guideNumber.toString(), customerName = customeValue?.name ?: "Nombre vacío",
                    customerContactNumber = customeValue?.contactNumber ?: "Número vacío")
        }
        return pdfDocumentGenerator.getPdf(data)
    }

    private fun mergePdfs(urls: List<String>, consolidatePdf:ByteArrayInputStream?): ByteArrayInputStream? {
        val merger = PDFMergerUtility()
        val out = ByteArrayOutputStream()
        for(url:String in urls){
            if(url==null || url.isBlank()){
                continue
            }
            val actualFile = readFileFromUrl(url)
            merger.addSource(actualFile)
        }
        consolidatePdf?.let { merger.addSource(consolidatePdf) }
        merger.destinationStream=out
        merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly())
        return ByteArrayInputStream(out.toByteArray())
    }

    private fun readFileFromUrl(url:String): InputStream? {
        val url = URL(url)
        val baos = ByteArrayOutputStream()
        url.openStream().transferTo(baos)
        return ByteArrayInputStream(baos.toByteArray())
    }
}
