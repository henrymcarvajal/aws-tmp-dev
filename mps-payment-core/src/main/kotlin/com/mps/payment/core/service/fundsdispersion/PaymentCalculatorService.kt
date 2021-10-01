package com.mps.payment.core.service.fundsdispersion

import com.mps.payment.core.model.DeliveredOrder
import com.mps.payment.core.model.MerchantInfo
import com.mps.payment.core.repository.MerchantType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class PaymentCalculatorService {

    @Value("\${mps.fees.bankTransfer:6000}")
    lateinit var mpsBankTransferFee: String

    private companion object {
        const val BANK_TRANSFER_FEE_NAME = "Transferencias bancarias" //"BANK_TRANSFER_FEE"
        const val BASE_PRICE = "Precio proveedor" //"BASE_PRICE"
        const val FREIGHT_COST = "Valor flete" //"FREIGHT_COST"
        const val BROKERING_TAX = "Comisión MPS" //"BROKERING_TAX"
    }

    fun calculatePayments(consolidatedMerchants: Map<MerchantInfo, MutableList<DeliveredOrder>>) {
        consolidatedMerchants.forEach { (k, v) ->
            calculatePayment(k, v)
        }
    }

    private fun calculatePayment(merchantInfo: MerchantInfo, collections: MutableList<DeliveredOrder>) {
        collections.forEach {
            it.applicableCharges = getApplicableChargesPerDelivery(merchantInfo.type, it)
            it.totalCharges = calculateTotal(it.applicableCharges)
            it.totalPayment = it.collectedAmount?.minus(it.totalCharges)!!
            merchantInfo.paymentTotal += it.totalPayment
        }
        merchantInfo.applicableCharges = getApplicableChargesPerMerchant(merchantInfo)
        merchantInfo.chargesTotal = calculateTotal(merchantInfo.applicableCharges)
        merchantInfo.paymentTotal -= merchantInfo.chargesTotal
    }

    private fun getApplicableChargesPerMerchant(merchantInfo: MerchantInfo): Map<String, Int> {
        val applicableCharges = mutableMapOf<String, Int>()
        applicableCharges[BANK_TRANSFER_FEE_NAME] = mpsBankTransferFee.toInt()
        return applicableCharges
    }

    private fun getApplicableChargesPerDelivery(merchantType: MerchantType, collection: DeliveredOrder): Map<String, Int> {
        val applicableCharges = mutableMapOf<String, Int>()
        return when (merchantType) {
            MerchantType.DROPPER -> {
                applicableCharges[BASE_PRICE] = collection.productBasePrice
                applicableCharges[FREIGHT_COST] = collection.freightTotalCost
                applicableCharges[BROKERING_TAX] = collection.brokeringFee
                applicableCharges
            }
            MerchantType.PROVIDER -> {
                applicableCharges[BASE_PRICE] = collection.productBasePrice
                applicableCharges
            }
            MerchantType.NONE -> applicableCharges
        }
    }

    private fun calculateTotal(values: Map<String, Int>): Int {
        var total = 0
        values.forEach {
            total += it.value
        }
        return total
    }
}

