package com.mps.payment.core.model

import com.mps.payment.core.repository.MerchantType
import org.apache.poi.ss.usermodel.Sheet
import java.util.*

data class BankAccountInfo(
    val number: String,
    val type: String,
    val bankName: String,
)

data class MerchantInfo(
    val email: String,
    val name: String,
    val NIT: String,
    val type: MerchantType = MerchantType.NONE,
) {
    lateinit var accountInfo: BankAccountInfo
    var chargesTotal: Int = 0
    var paymentTotal: Int = 0
    var applicableCharges: Map<String, Int> = mutableMapOf()
}

class DeliveredOrder(
    val guideNumber: Int,
    private val deliveryState: String,
    val collectedAmount: Int?,
    val freightTotalCost: Int = 0
) {
    var id: UUID? = null
    var productName: String = ""
    var productDropshippingPrice: Int = 0
    var productBasePrice: Int = 0
    var brokeringFee: Int = 0
    var totalCharges: Int = 0
    var totalPayment: Int = 0

    var applicableCharges: Map<String, Int> = mutableMapOf()
    override fun toString(): String {
        return "DeliveredOrder(guideNumber=$guideNumber, deliveryState='$deliveryState', collectedAmount=$collectedAmount, freightTotalCost=$freightTotalCost, id=$id, productName='$productName', productDropshippingPrice=$productDropshippingPrice, productBasePrice=$productBasePrice, brokeringFee=$brokeringFee, totalCharges=$totalCharges, totalPayment=$totalPayment, applicableCharges=$applicableCharges)"
    }
}