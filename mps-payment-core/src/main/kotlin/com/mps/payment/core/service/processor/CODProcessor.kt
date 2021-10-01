package com.mps.payment.core.service.processor

import com.mps.common.dto.OrderDropDTO
import com.mps.payment.core.enum.OrderStatus
import com.mps.payment.core.model.Customer
import com.mps.payment.core.model.GeneralOrderDrop
import com.mps.payment.core.model.Merchant
import com.mps.payment.core.model.Product
import com.mps.payment.core.service.CommunicationService
import com.mps.payment.core.service.TEMPLATE_ORDER_CONFIRMED
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class CODProcessor(private val communicationService: CommunicationService) : OrderProcessorTemplate() {

    @Value("\${fe.url}")
    var  url: String="htttp://test.com"

    override fun process(orderDropDTO: OrderDropDTO?, product: Product?, generalOrderDropEntity: GeneralOrderDrop?,
                         customer: Customer?, sellerMerchant: Merchant?) {
        generalOrderDropEntity!!.comision = BigDecimal(650)
        generalOrderDropEntity!!.orderStatus = OrderStatus.TO_BE_CONFIRMED.state
        val urlToConfirm ="${url}customer?id=${generalOrderDropEntity.id}"
        val sms= "Por favor confirma tu orden abriendo el siguiente enlace: $urlToConfirm"
        val emailMessage= "Por favor confirma tu pedido, usando el siguiente botón:"
        val subject = "Confirma tu pedido"
        communicationService.sendSmSAndEmail(email = customer!!.email,
                contactNumber = customer!!.contactNumber,smsMessage = sms,
                urlToConfirm,template = TEMPLATE_ORDER_CONFIRMED,
                emailMessage = emailMessage,title = subject,subject = subject,actionText = "Confirmar pedido"
        )
    }

    fun confirmationProcess(order:GeneralOrderDrop){
        inventoryProcessorService.decreaseInventoryCOD(order.dropShippingSale.product,order)
    }
}