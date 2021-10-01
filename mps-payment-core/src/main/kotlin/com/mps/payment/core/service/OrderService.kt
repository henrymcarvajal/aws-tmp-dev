package com.mps.payment.core.service

import com.mps.common.dto.*
import com.mps.payment.core.enum.OrderStatus
import com.mps.payment.core.enum.PaymentMethod
import com.mps.payment.core.model.*
import com.mps.payment.core.repository.OrderRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

const val MESSAGE_GUIDE_NUMBER_NOTIFICATION = "Tu orden ha sido generada satisfactoriamente. Puedes realizar seguimiento de tu orden en el siguiente enlace "
const val SUBJECT_ORDER_GENERATION = "Consulta el estado de tu pedido"
val FREIGHT_PRICE_PROM = BigDecimal(10000)

/**
 * TODO order.productid = checkout id for drop order, we have to add extra logic for supporting no drop order
 */

@Service
class OrderService(
        private val orderRepository: OrderRepository,
        private val productService: ProductService,
        private val customerService: CustomerService,
        private val logisticPartnerService: LogisticPartnerService,
        private val dropshippingSaleService: DropshippingSaleService,
        private val priceService: PriceService,
        private val inventoryProcessorService: InventoryProcessorService,
        private val communicationService: CommunicationService
) {
    @Value("\${fe.url}")
    var  url: String="htttp://test.com"

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun createOrder(orderDropDTO: OrderDropDTO): GenericResponse<*> {
        val dropSale = dropshippingSaleService.getDropshippingSaleById(orderDropDTO.productId)
        if(dropSale.isEmpty()){
            return GenericResponse.ErrorResponse("No existe el producto")
        }
        val customer = customerService.createOrReplaceCustomer(orderDropDTO.customer!!)
        val orderEntity = orderDropDTO.toEntity(dropShippingSale = dropSale[0],customerId = customer.id!!)
        orderDropDTO.id = orderEntity.id
        orderEntity.customerId = customer.id!!
        orderEntity.dropShippingSale = dropSale[0]
        var merchantSeller = dropSale[0].merchant
        val product = dropSale[0].product
        val basePrice = dropSale[0].amount ?: product.amount
        orderDropDTO.amount = priceService.getOrderPrice(dropSale[0].id, orderDropDTO.quantity, basePrice, dropSale[0].specialConditions)
        orderEntity.amount = orderDropDTO.amount
        val pairResult = inventoryProcessorService.processPrivateInventoryForOrder(customer.city!!,
                orderDropDTO.quantity,product.id,dropSale[0].merchant.id!!,product.inventory)
        val branchCode = pairResult.first ?: return return processPreOrderError(orderEntity, "There is not inventory")
        orderDropDTO.branchCode = branchCode
        orderEntity.branchCode = branchCode
        processPayment(orderDropDTO, product, orderEntity, customer, merchantSeller)
        val order = orderRepository.save(orderEntity)
        return processResponse(order, product,pairResult.second)
    }

    private fun processResponse(order: GeneralOrderDrop, product: Product,privateInventory: PrivateInventory?): GenericResponse<*> {
        if (OrderStatus.FAILED.state == order.orderStatus) {
            return GenericResponse.ErrorResponse("Error processing order")
        }
        return if (PaymentMethod.COD.method == order.paymentMethod || PaymentMethod.ONLINE.method == order.paymentMethod) {
            inventoryProcessorService.decreaseInventory(product, order,privateInventory)
            GenericResponse.SuccessResponse(order.toDTO())
        } else {
            if (order.paymentMethod == PaymentMethod.MPS.method && order.paymentId.isNullOrBlank()) {
                GenericResponse.ErrorResponse("Error generating payment from product")
            } else {
                inventoryProcessorService.decreaseInventory(product, order,privateInventory)
                GenericResponse.SuccessResponse(order.paymentId)
            }
        }
    }

    private fun processPayment(orderDropDTO: OrderDropDTO, product: Product, generalOrderDropEntity: GeneralOrderDrop,
                               customer: Customer, sellerMerchant: Merchant) {
        when {
            PaymentMethod.COD.method == orderDropDTO.paymentMethod -> {
                generalOrderDropEntity.comision = BigDecimal(650)
                generalOrderDropEntity.orderStatus = OrderStatus.TO_BE_CONFIRMED.state
                val urlToConfirm ="${url}customer?id=${orderDropDTO.id}"
                val sms= "Por favor confirma tu orden abriendo el siguiente enlace: $urlToConfirm"
                val emailMessage= "Por favor confirma tu pedido, usando el siguiente botón:"
                val subject = "Confirma tu pedido"
                communicationService.sendSmSAndEmail(email = customer.email,
                        contactNumber = customer.contactNumber,smsMessage = sms,urlToConfirm,template = TEMPLATE_ORDER_CONFIRMED,
                        emailMessage = emailMessage,title = subject,subject = subject,actionText = "Confirmar pedido"
                )
            }
            PaymentMethod.MPS.method == orderDropDTO.paymentMethod -> {
                generalOrderDropEntity.comision = BigDecimal.ZERO
                val paymentAgree = PaymentAgree(
                        idPayment = product.id.toString().takeLast(6),
                        customer = customer.toDTO()
                )
                when (val paymentResponse = productService.createPaymentFromProduct(paymentAgree, orderDropDTO.amount,sellerMerchant.id)) {
                    is GenericResponse.SuccessResponse -> {
                        generalOrderDropEntity.paymentId = paymentResponse.obj as String
                        generalOrderDropEntity.orderStatus = OrderStatus.PAYMENT_PENDING.state
                    }
                    is GenericResponse.ErrorResponse -> {
                        log.error("error creating payment from product")
                        generalOrderDropEntity.orderStatus = OrderStatus.FAILED.state
                    }
                }
            }
            PaymentMethod.ONLINE.method == orderDropDTO.paymentMethod -> {
                generateFreight(generalOrderDropEntity, product, customer, orderDropDTO, sellerMerchant, true)
            }
        }
    }

    private fun generateFreight(generalOrderDropEntity: GeneralOrderDrop,
                                product: Product, customer: Customer,
                                orderDropDTO: OrderDropDTO, sellerMerchant: Merchant, isOnline: Boolean = false) {
        generalOrderDropEntity.comision = BigDecimal(550)
        when (val response = logisticPartnerService.requestFreightCOD(sellerMerchant, customer = customer, orderDropDTO,
                product.name!!, isOnline)) {
            is GenericResponse.SuccessResponse -> {
                generalOrderDropEntity.label
                generalOrderDropEntity.guideNumber = (response.obj as String).toInt()
                generalOrderDropEntity.orderStatus = OrderStatus.TO_DISPATCH.state

            }
            is GenericResponse.ErrorResponse -> {
                generalOrderDropEntity.orderStatus = OrderStatus.TO_DISPATCH.state
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

    private fun processPreOrderError(generalOrderDropEntity: GeneralOrderDrop, message: String): GenericResponse.ErrorResponse {
        generalOrderDropEntity.orderStatus = OrderStatus.FAILED.state
        orderRepository.save(generalOrderDropEntity)
        return GenericResponse.ErrorResponse(message)
    }

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

    fun saveAll(order: List<GeneralOrderDrop>) = orderRepository.saveAll(order)
}