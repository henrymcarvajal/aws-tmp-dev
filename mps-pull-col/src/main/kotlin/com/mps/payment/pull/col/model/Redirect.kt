package com.mps.payment.pull.col.model

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

data class RedirectInformation(
        val id: String,
        val signature: String,
        val transactionDate: String,
        val version:String,
        val siteId:String,
        val actionMode:String,
        val amount:String,
        val environment: String,
        val currency: String,
        val paymentConfig : String,
        val action: String,
        val method:String,
        val orderId:String
)

@Entity
@Table(name = "payment_partner")
data class PaymentPartner(@Id val id: String, @Column(name = "amount") val amount: String,
                          @Column(name = "transaction_date") var transactionDate: String,
                          @Column(name = "payment_method") var paymentMethod: String? = null,
                          @Column(name = "signature") var signature: String?=null,
                          @Column(name = "modificationdate") var modificationDate: LocalDateTime= LocalDateTime.now(),
                          @Column(name = "final_status") var finalStatus: String?=null
) {
    constructor() : this("", "","","") {

    }
}