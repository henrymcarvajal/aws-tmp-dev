package com.mps.payment.pull.col.util

import com.mps.common.dto.PaymentDTO
import com.mps.payment.pull.col.model.PaymentPartner
import com.mps.payment.pull.col.model.RedirectInformation
import java.math.BigDecimal
import java.util.*

fun createRedirectTest(paymentId:String="123456"):RedirectInformation=
        RedirectInformation(paymentId,"","","","","",
                "","","","","","","")

fun createPaymentPartner()=PaymentPartner()

fun createPaymentTest(merchantId: UUID = UUID.randomUUID(), id: UUID = UUID.randomUUID(),
                      amount : BigDecimal = BigDecimal.valueOf(100000), idState:Int=1) = PaymentDTO(
        amount = amount, id = id, idState = idState, idCustomer = UUID.randomUUID(),
        idMerchant = merchantId, linkUrl = "https://payco.link/123455",
        guideNumber = "1224455", transportCompany = "servidelivery",description = "desc"
)