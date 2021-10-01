package com.mps.payment.core.service.fundsdispersion

import com.mps.common.dto.ServiceResponse
import com.mps.payment.core.enum.OrderStatus
import com.mps.payment.core.model.BankAccountInfo
import com.mps.payment.core.model.DeliveredOrder
import com.mps.payment.core.model.MerchantInfo
import com.mps.payment.core.repository.MappedGuideNumber
import com.mps.payment.core.repository.MappedGuideNumberRepository
import com.mps.payment.core.repository.MerchantType
import com.mps.payment.core.service.OrderService
import com.mps.payment.core.service.exceltools.ExcelService
import com.mps.payment.core.service.fundsdispersion.file.FundsDispersionFileService
import com.mps.payment.core.util.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal


@Service
class FundsDispersionService(
    private val deliveryExtractorService: DeliveryExtractorService,
    private val mappedGuideNumberRepository: MappedGuideNumberRepository,
    private val generalOrderService: OrderService,
    private val excelService: ExcelService,
    private val paymentCalculatorService: PaymentCalculatorService,
    private val fundsDispersionFileService: FundsDispersionFileService,
    private val fundsDispersionMailService: FundsDispersionMailService
) {

    companion object {
        const val EXCEL_FILE_NULL_ERR_MESSAGE = "El archivo Excel le must not be null"
        const val UNEXPECTED_ERROR_ERR_MESSAGE = "Error inesperado al generar la dispersion"
    }

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun generateDispersion(multipartFile: MultipartFile?): ServiceResponse<*> {
        try {
            if (multipartFile != null) {
                when (val deliveredOrdersResponse = deliveryExtractorService.extractDeliveries(multipartFile)) {
                    is ServiceResponse.Success -> {
                        val deliveredOrders = deliveredOrdersResponse.obj as List<*>

                        val consolidatedMerchants = consolidateMerchants(deliveredOrders as List<DeliveredOrder>)
                        paymentCalculatorService.calculatePayments(consolidatedMerchants)
                        val dispersionWorkbooks = fundsDispersionFileService.generateDispersionWorkbooks(consolidatedMerchants)

                        fundsDispersionMailService.sendFundsDispersionEmails(dispersionWorkbooks.details)
                        updateFreightCost(deliveredOrders)

                        return ServiceResponse.Success(excelService.writeWorkbook(dispersionWorkbooks.summary))
                    }
                    is ServiceResponse.Error ->
                        return ServiceResponse.Error(deliveredOrdersResponse.message)
                }
            } else {
                return ServiceResponse.Error(EXCEL_FILE_NULL_ERR_MESSAGE)
            }
        } catch (e: Exception) {
            log.error("error", e)
            return ServiceResponse.Error(UNEXPECTED_ERROR_ERR_MESSAGE)
        }
    }

    private fun updateFreightCost(deliveredOrders: List<DeliveredOrder>) {
        deliveredOrders.forEach {
            if (it.id != null) {
                val generalOrderSearch = generalOrderService.findById(it.id!!)
                if (generalOrderSearch.isPresent) {
                    val generalOrder = generalOrderSearch.get()
                    generalOrder.freightPrice = BigDecimal.valueOf(it.freightTotalCost.toLong())
                    generalOrder.orderStatus = OrderStatus.TRANSFERRED.state
                    generalOrderService.saveAll(listOf(generalOrder))
                }
            }
        }
    }

    private fun getOrdersInfo(collections: List<DeliveredOrder>): List<MappedGuideNumber> {
        val guideList = ArrayList<Int>()
        try {
            collections.forEach {
                guideList.add(it.guideNumber)
            }
            return mappedGuideNumberRepository.findAllByGuideNumber(guideList)
        } catch (e: Exception) {
            println(ExceptionUtils.toString(e))
        }
        return emptyList()
    }

    private fun createMerchant(mappedGuideNumber: MappedGuideNumber): MerchantInfo {
        val merchantInfo = MerchantInfo(
            email = mappedGuideNumber.merchantEmail,
            name = mappedGuideNumber.merchantName,
            NIT = mappedGuideNumber.merchantNIT,
            type = MerchantType.valueOf(mappedGuideNumber.merchantType)
        )

        val accountInfo = BankAccountInfo(
            number = mappedGuideNumber.accountNumber,
            type = mappedGuideNumber.accountType,
            bankName = mappedGuideNumber.accountBank
        )

        merchantInfo.accountInfo = accountInfo

        return merchantInfo
    }

    private fun fillProductInfo(deliveredOrder: DeliveredOrder, mappedGuideNumber: MappedGuideNumber) {
        deliveredOrder.productName = mappedGuideNumber.productName
        deliveredOrder.productBasePrice = mappedGuideNumber.productBasePrice!!
        deliveredOrder.productDropshippingPrice = mappedGuideNumber.productDropshippingPrice!!

        deliveredOrder.brokeringFee = mappedGuideNumber.brokeringFee!!
    }

    private fun consolidateMerchants(deliveredOrders: List<DeliveredOrder>): Map<MerchantInfo, MutableList<DeliveredOrder>> {

        val consolidatedCollections = HashMap<MerchantInfo, MutableList<DeliveredOrder>>()

        val mappedOrders = getOrdersInfo(deliveredOrders)

        deliveredOrders.forEach {
            val mappedGuideNumber = mappedOrders.find { mappedOrder ->
                mappedOrder.guideNumber == it.guideNumber
            }
            if (mappedGuideNumber != null) {
                it.id = mappedGuideNumber.id
                val merchantInfo = createMerchant(mappedGuideNumber)
                fillProductInfo(it, mappedGuideNumber)
                if (!consolidatedCollections.contains(merchantInfo)) {
                    consolidatedCollections[merchantInfo] = mutableListOf()
                }
                consolidatedCollections[merchantInfo]?.add(it)
            }
        }

        return consolidatedCollections
    }
}


