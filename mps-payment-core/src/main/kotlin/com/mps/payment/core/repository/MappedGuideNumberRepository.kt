package com.mps.payment.core.repository

import com.mps.payment.core.model.GeneralOrderDrop
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface MappedGuideNumberRepository : JpaRepository<GeneralOrderDrop, UUID> {

    companion object {
        private const val BASIC_COLUMNS = "" +
                " CAST(go2.id AS text) as \"id\"," +
                " go2.guide_number as \"guideNumber\"," +
                " m.name as \"merchantName\"," +
                " m.email as \"merchantEmail\"," +
                " m.nit as \"merchantNIT\"," +
                " m.account_bank as \"accountBank\"," +
                " m.account_type as \"accountType\"," +
                " m.account_number as \"accountNumber\"," +
                " p.name as \"productName\"," +
                " p.precio_dropshipping as \"productDropshippingPrice\"," +
                " go2.amount as \"productBasePrice\"," +
                " go2.comision as \"brokeringFee\","

        private const val query = "select " +
                BASIC_COLUMNS +
                " 'DROPPER' as \"merchantType\"" +
                " from general_order go2" +
                " inner join dropshipping_sale ds on ds.id= go2.product_id" +
                " inner join merchant m on m.id = ds.seller_merchant_id " +
                " inner join product p on p.id = ds.product_id " +
                " where go2.guide_number in :guideNumbers" +
                " and go2.order_status != 5" +
                " union " +
                "select " +
                BASIC_COLUMNS +
                " 'PROVIDER' as \"merchantType\"" +
                " from general_order go2" +
                " inner join product p on p.id =  go2.product_id" +
                " inner join merchant m on m.id = p.merchant_id" +
                " where go2.guide_number in :guideNumbers" +
                " and go2.order_status != 5"
    }

    @Query(value = query, nativeQuery = true)
    fun findAllByGuideNumber(guideNumbers: List<Int>): List<MappedGuideNumber>
}

interface MappedGuideNumber {
    val id: UUID
    val guideNumber: Int
    val merchantName: String
    val merchantNIT: String
    val merchantEmail: String
    val merchantType: String
    val accountNumber: String
    val accountType: String
    val accountBank: String
    val productName: String
    val productBasePrice: Int?
    val productDropshippingPrice: Int?
    val brokeringFee: Int?
}

enum class MerchantType(val type: String) {
    PROVIDER("PROVIDER"),
    DROPPER("DROPPER"),
    NONE("NONE")
}
