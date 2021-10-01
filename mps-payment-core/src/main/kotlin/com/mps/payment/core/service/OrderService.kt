package com.mps.payment.core.service

import com.mps.common.dto.*
import com.mps.payment.core.enum.OrderStatus
import com.mps.payment.core.model.*
import com.mps.payment.core.repository.OrderRepository
import com.mps.payment.core.service.factory.OrderProcessorFactory
import com.mps.payment.core.service.processor.CODProcessor
import com.mps.payment.core.service.processor.PreProcessOrderResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

const val MESSAGE_GUIDE_NUMBER_NOTIFICATION = "Tu orden ha sido generada satisfactoriamente. Puedes realizar seguimiento de tu orden en el siguiente enlace "
const val SUBJECT_ORDER_GENERATION = "Consulta el estado de tu pedido"
val FREIGHT_PRICE_PROM = BigDecimal(10000)

/**
 * TODO for own products is necessary a different table
 */

@Service
class OrderService(
        private val orderRepository: OrderRepository,
        private val productService: ProductService,
        private val customerService: CustomerService,
        private val logisticPartnerService: LogisticPartnerService,
        private val dropshippingSaleService: DropshippingSaleService,
        private val orderProcessorFactory: OrderProcessorFactory
) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun createOrder(orderDropDTO: OrderDropDTO): GenericResponse<*> {
        val orderProcessor = orderProcessorFactory.selectProcessor(orderDropDTO.paymentMethod)
                ?: return GenericResponse.ErrorResponse("Invalid payment method")
        return when(val response = orderProcessor.preProcessOrder(orderDropDTO)){
            is GenericResponse.SuccessResponse->{
                val data = response.obj as PreProcessOrderResponse
                val orderEntity = data.orderEntity
                orderProcessor.process(orderDropDTO=data.orderDTO,product = data.product,generalOrderDropEntity = orderEntity,
                customer = data.customer,sellerMerchant = data.sellerMerchant)
                val order = orderRepository.save(orderEntity)
                orderProcessor.postOrder(order = order,product = data.product,privateInventory = data.privateInventory)
            }
            is GenericResponse.ErrorResponse->{
               response
            }
        }
    }

    fun updateGuideToOrder(paymentId: String, guideNumber: Int, label:String): OrderDropDTO {
        val order = orderRepository.getByPaymentId(paymentId).get()
        order.label = label
        order.guideNumber = guideNumber
        order.orderStatus = OrderStatus.TO_DISPATCH.state
        return orderRepository.save(order).toDTO()
    }

    fun findByPaymentId(paymentId: String) = orderRepository.getByPaymentId(paymentId)


    fun getOrdersForProvider(merchantId: UUID, queryParams: QueryParams): OrderViewConsolidate {
        val dropProducts = productService.getDropProductsForMerchant(merchantId)
        if (dropProducts.isEmpty()) {
            log.warn("getOrdersForDropshipperSeller: there is no drop prducts for this merchant $merchantId")
            return OrderViewConsolidate(
                    orders = listOf(),
                    totalOrderAmountByStatus = BigDecimal.ZERO,
                    totalProfitSaleByStatus = BigDecimal.ZERO,
                    totalOrderByStatus = 0,
                    actualPage = 0,
                    totalRecords = 0,
                    totalPages = 0
            )
        }

        val productsId = dropProducts.map { it.id }
        val dropshippingSalesProduct = dropshippingSaleService.findByProductIdInAndDisabledFalse(productsId)
        val dropshippingSaleIds = dropshippingSalesProduct.map { it.id }
        val initialDate = LocalDateTime.now().minusDays(queryParams.duration.toLong())
        val paginatedResponse = orderRepository.findOrdersByProductsAndtDynamicParams(dropshippingSaleIds, initialDate,
                queryParams.state, queryParams.guideNumber,queryParams.pageNumber,null,100)
        val dropOrders = paginatedResponse.records
                .map {
                    var customerName = ""
                    val customer = customerService.getCustomerById(it.customerId!!)
                    if (customer.isEmpty) {
                        log.error("order does not have customer ${it.id}")
                    } else {
                        customerName = customer.get().name
                    }
                    val productId = dropshippingSalesProduct.filter { dropshippingSale -> dropshippingSale.id == it.dropShippingSale.id }[0].product.id
                    val productRecord = dropProducts.filter { product -> product.id == productId }[0]
                    OrderViewProvider(
                            sellPrice = BigDecimal(it.quantity) * (productRecord.dropshippingPrice ?: BigDecimal.ZERO),
                            orderId = it.id,
                            orderState = it.orderStatus,
                            creationDate = it.creationDate,
                            customerName = customerName,
                            freightPrice = it.freightPrice ?: FREIGHT_PRICE_PROM,
                            guideNumber = it.guideNumber,
                            productName = productRecord.name ?: "Sin nombre",
                            isLabeled = it.isLabeled
                    )
                }
        val totalOrders = dropOrders.size
        val totalEarnMoney = BigDecimal.ZERO
        val totalAmount = dropOrders.fold(BigDecimal.ZERO)
        { acc, orderViewDropshipper -> orderViewDropshipper.sellPrice + acc }

        return OrderViewConsolidate(
                orders = dropOrders,
                totalOrderAmountByStatus = totalAmount,
                totalProfitSaleByStatus = totalEarnMoney,
                totalOrderByStatus = totalOrders,
                actualPage = paginatedResponse.pageNumber,
                totalRecords = paginatedResponse.totalRecords,
                totalPages = paginatedResponse.totalPages
        )
    }

    fun getOrderCompleteById(id: UUID): Optional<OrderDropDTO> {
        val order = orderRepository.findById(id)
        if (order.isEmpty) {
            return Optional.empty<OrderDropDTO>()
        }
        val orderValue = order.get()
        orderValue.dropShippingSale.merchant.name
        val customer = customerService.getCustomerById(orderValue.customerId)
        val finalOrder = order.get().toDTO()
        finalOrder.customer = customer.get().toDTO()
        return Optional.of(finalOrder)
    }

    fun getOrdersForDropshipperSeller(merchantId: UUID, queryParams: QueryParams): OrderViewConsolidate {

        val dropshihppingProducts = dropshippingSaleService.getDropshippingSaleBySellerMerchantId(merchantId, true)
        if (dropshihppingProducts.isEmpty()) {
            log.warn("getOrdersForDropshipperSeller: there is no drop prducts for this merchant $merchantId")
            return OrderViewConsolidate(
                    orders = listOf(),
                    totalOrderAmountByStatus = BigDecimal.ZERO,
                    totalProfitSaleByStatus = BigDecimal.ZERO,
                    totalOrderByStatus = 0,
                    totalRecords = 0,
                    actualPage = 0,
                    totalPages = 0
            )
        }
        val checkoutId = dropshihppingProducts.map { it.id }
        val initialDate = LocalDateTime.now().minusDays(queryParams.duration.toLong())
        val customer = queryParams.customerNumberContact?.let {
            customerService.getByContactNumber(it.toString())
        }
        val paginatedResponse = orderRepository.findOrdersByProductsAndtDynamicParams(checkoutId, initialDate,
                queryParams.state, queryParams.guideNumber,queryParams.pageNumber,customer?.get()?.id,15)
        val dropOrders = mappingOrdersToView(paginatedResponse.records, dropshihppingProducts)
        val totalOrders = dropOrders.size
        val totalEarnMoney = dropOrders.fold(BigDecimal.ZERO)
        { acc, orderViewDropshipper -> orderViewDropshipper.profitSale + acc }
        val totalAmount = dropOrders.fold(BigDecimal.ZERO)
        { acc, orderViewDropshipper -> orderViewDropshipper.sellPrice + acc }

        return OrderViewConsolidate(
                orders = dropOrders,
                totalOrderAmountByStatus = totalAmount,
                totalProfitSaleByStatus = totalEarnMoney,
                totalOrderByStatus = totalOrders,
                totalRecords = paginatedResponse.totalRecords,
                actualPage = paginatedResponse.pageNumber,
                totalPages = paginatedResponse.totalPages
        )
    }

    fun updateOrder(request: UpdateOrderRequest):CustomerDTO?{
        val optionalOrder = orderRepository.findById(request.id)
        if(optionalOrder.isEmpty){
            log.error("order does not exist ${request.id}")
            return null
        }
        var existingOrder = optionalOrder.get()
        if(existingOrder.orderStatus != OrderStatus.TO_BE_CONFIRMED.state){
            log.error("Incorrect status ${request.id}")
            return null
        }
        val customer =  customerService.createOrReplaceCustomer(request.customer)
        if(existingOrder.customerId != customer.id){
            existingOrder.customerId = customer.id!!
            existingOrder = orderRepository.save(existingOrder)
        }
        logisticPartnerService.generateFreightByOrder(order = existingOrder,customer = customer)
        val processor = orderProcessorFactory.selectProcessor(existingOrder.paymentMethod)!! as CODProcessor
        processor.confirmationProcess(existingOrder)
        return customer.toDTO()
    }

    private fun mappingOrdersToView(orders: List<GeneralOrderDrop>,
                                    dropshihppingProducts: List<ProductDropSale>): List<OrderViewDropshipper> {
        return orders
                .map {
                    var (customerName, customerPhone) = gettingCustomerInfo(it)
                    val productSaleInfo = dropshihppingProducts.filter { productDropSale ->
                        productDropSale.id == it
                                .dropShippingSale.id
                    }[0]
                    OrderViewDropshipper(
                            buyPrice = BigDecimal(it.quantity) * productSaleInfo.buyPrice,
                            sellPrice = it.amount,
                            orderId = it.id,
                            orderState = it.orderStatus,
                            creationDate = it.creationDate,
                            customerName = customerName,
                            freightPrice = it.freightPrice ?: FREIGHT_PRICE_PROM,
                            guideNumber = it.guideNumber,
                            productName = productSaleInfo.name,
                            profitSale = calculateProfitPerSale(productSaleInfo, it.freightPrice, it.quantity),
                            customerPhone = customerPhone
                    )
                }
    }

    private fun gettingCustomerInfo(it: GeneralOrderDrop): Pair<String, String> {
        var customerName = ""
        var customerPhone = ""
        val customer = customerService.getCustomerById(it.customerId!!)
        if (customer.isEmpty) {
            log.error("order does not have customer ${it.id}")
        } else {
            customerName = customer.get().name
            customerPhone = customer.get().contactNumber
        }
        return Pair(customerName, customerPhone)
    }

    private fun calculateProfitPerSale(sale: ProductDropSale, freightPrice: BigDecimal?, quantity: Int): BigDecimal {
        val finalFreightPrice = freightPrice ?: FREIGHT_PRICE_PROM
        return sale.sellPrice.minus(sale.buyPrice * BigDecimal(quantity)).minus(finalFreightPrice)
    }

    fun findById(id: UUID) = orderRepository.findById(id)

    fun saveAll(order: List<GeneralOrderDrop>): MutableIterable<GeneralOrderDrop> = orderRepository.saveAll(order)
}