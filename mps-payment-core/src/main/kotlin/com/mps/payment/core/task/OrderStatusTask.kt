package com.mps.payment.core.task

import com.mps.payment.core.client.entregalo.EntregaloClient
import com.mps.payment.core.client.entregalo.payload.QueryServiceStatusResponse
import com.mps.payment.core.client.entregalo.payload.QueryStatusRequest
import com.mps.payment.core.email.EmailSender
import com.mps.payment.core.enum.OrderStatus
import com.mps.payment.core.model.GeneralOrderDrop
import com.mps.payment.core.repository.InternalNumberRepository
import com.mps.payment.core.repository.OrderRepository
import com.mps.payment.core.service.CONST_MESSAGE
import com.mps.payment.core.service.CONST_TITLE_BODY
import com.mps.payment.core.service.SUBJECT_PRODUCT_REMOVED
import com.mps.payment.core.service.TEMPLATE_EMAIL_PLANE_TEXT
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime


const val FREIGHT_NATIONAL_COMMISSION = 5
const val NEW_NOVELTY_SUBJECT = "Tienes una nueva novedad en un pedido"

@Component
class OrderStatusTask(private val orderRepository: OrderRepository, private val entregaloClient: EntregaloClient,
                      private val internalNumberRepository: InternalNumberRepository,
                    private val emailSender: EmailSender) {

    var log: Logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    var cacheManager: CacheManager? = null


    @Scheduled(fixedRate = 36000000)
    fun run() {
        cacheManager?.getCache("cities")?.clear()
        try {
            val orders = orderRepository.findByOrderStatusIsNotAndGuideNumberIsNotNull(OrderStatus.DELIVERED.state)
            if (orders.isEmpty()) {
                log.info("There is not record without FreightPrice")
            }
            orders.forEach { order ->
                val request = QueryStatusRequest(idShipping = order.guideNumber!!)
                val response = entregaloClient.sendQueryServiceStatusRequest(request)
                if (response.status == HttpStatus.OK || response.status == HttpStatus.CREATED) {
                    updateFreightPrice(order, response)
                    order.orderStatus = updateGuideState(response.serviceStatus)
                    if (order.orderStatus == OrderStatus.NOTICE.state) {
                        emailSender.sendEmailWithTemplate(receiver = order.dropShippingSale.merchant.email,
                                templateName = TEMPLATE_EMAIL_PLANE_TEXT, title = NEW_NOVELTY_SUBJECT,
                                o = mapOf(CONST_MESSAGE to "Uno de tus pedidos presenta novedad (número de guia: ${order.guideNumber}})." +
                                        " Te recomendamos que te pongas en contacto con tu cliente y escribas al whatsapp 3107626875 " +
                                        "para notificar el acuerdo con tu cliente.",
                                        CONST_TITLE_BODY to "Soluciona tu novedad lo más pronto"))
                    }
                    orderRepository.save(order)
                } else {
                    log.error("Error calling entregalo for updating order ${order.id}")
                }
            }
        } catch (e: Exception) {
            log.error("error executing order task", e)
        }

    }

    private fun updateFreightPrice(order: GeneralOrderDrop, response: QueryServiceStatusResponse) {
        if (order.freightPrice == null || order.freightPrice == BigDecimal.ZERO) {
            order.freightPrice = response.freightPrice
            if (response.freightPrice == null) {
                log.error("freight price from entregalo is null")
            }
            val internalNumber = internalNumberRepository.findByOrderId(order.id)
            val freightCommission = response.freightPrice
                    ?.multiply(BigDecimal(FREIGHT_NATIONAL_COMMISSION))?.divide(BigDecimal.valueOf(100))
            if (internalNumber.isEmpty()) {
                log.error("No exist record for ${order.id} in internal number")
            } else {
                val internalNumberRecord = internalNumber[0]
                internalNumberRecord.freightCommission = freightCommission
                internalNumberRecord.modificationDate = LocalDateTime.now()
                internalNumberRepository.save(internalNumberRecord)
            }
        }
    }

    private fun updateGuideState(entregaloStatus: String?): Int {
        if (entregaloStatus == null) {
            log.error("Error in orderstatustask, entregaloStatus is null")
            return OrderStatus.TO_DISPATCH.state
        }
        return when (entregaloStatus) {
            "PendingCheck", "PendingCollect" -> {
                OrderStatus.TO_DISPATCH.state
            }
            "Process", "Ongoing", "InTransit" -> {
                OrderStatus.ON_DELIVERY.state
            }
            "Delivered" -> {
                OrderStatus.DELIVERED.state
            }
            "Canceled" -> {
                OrderStatus.CANCELLED.state
            }
            "Return" -> {
                OrderStatus.RETURN.state
            }
            "Notice", "Reprogrammed" -> {
                OrderStatus.NOTICE.state
            }
            else -> OrderStatus.FAILED.state
        }
    }
}