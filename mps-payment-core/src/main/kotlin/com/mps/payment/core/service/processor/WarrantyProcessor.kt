package com.mps.payment.core.service.processor

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.OrderDropDTO
import com.mps.payment.core.enum.OrderStatus
import com.mps.payment.core.model.Customer
import com.mps.payment.core.model.GeneralOrderDrop
import com.mps.payment.core.model.Merchant
import com.mps.payment.core.model.Product
import com.mps.payment.core.service.LogisticPartnerService
import org.springframework.stereotype.Service
import java.math.BigDecimal


@Service
class WarrantyProcessor(private val logisticPartnerService: LogisticPartnerService):OrderProcessorTemplate() {
    override fun process(orderDropDTO: OrderDropDTO?, product: Product?,
                         generalOrderDropEntity: GeneralOrderDrop?,
                         customer: Customer?, sellerMerchant: Merchant?) {
        generalOrderDropEntity!!.comision = BigDecimal(550)
        when (val response = logisticPartnerService.requestFreightCOD(sellerMerchant!!, customer = customer!!, orderDropDTO!!,
                product!!.name!!, true)) {
            is GenericResponse.SuccessResponse -> {
                generalOrderDropEntity!!.label
                generalOrderDropEntity.guideNumber = (response.obj as String).toInt()
                generalOrderDropEntity.orderStatus = OrderStatus.TO_DISPATCH.state

            }
            is GenericResponse.ErrorResponse -> {
                generalOrderDropEntity.orderStatus = OrderStatus.TO_DISPATCH.state
            }
        }
    }
}