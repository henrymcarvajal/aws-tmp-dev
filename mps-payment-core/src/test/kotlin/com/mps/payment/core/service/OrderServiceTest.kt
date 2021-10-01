package com.mps.payment.core.service



import com.mps.common.dto.GenericResponse
import com.mps.payment.core.model.QueryParams
import com.mps.payment.core.model.toDTO
import com.mps.payment.core.model.toEntity
import com.mps.payment.core.repository.OrderRepository
import com.mps.payment.core.repository.OrderRepositoryCustomImpl
import com.mps.payment.core.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.*
import java.math.BigDecimal
import java.util.*


internal class OrderServiceTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var productService: ProductService

    @Mock
    private lateinit var customerService: CustomerService

    @Mock
    private lateinit var logisticPartnerService: LogisticPartnerService

    @Mock
    private lateinit var communicationService: CommunicationService

    @Mock
    private lateinit var dropshippingSaleService: DropshippingSaleService

    @Mock
    private lateinit var priceService: PriceService

    @Mock
    private lateinit var inventoryProcessorService: InventoryProcessorService

    private lateinit var orderService: OrderService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        orderService = OrderService(orderRepository, productService, customerService,
                logisticPartnerService,dropshippingSaleService,priceService,inventoryProcessorService,communicationService)
    }

    @Test
    fun `product does not exist`() {
        val orderTest = createOrderDTOTest()
        Mockito.`when`(dropshippingSaleService.getDropshippingSaleById(orderTest.productId)).thenReturn(listOf())
        val response = orderService.createOrder(orderTest)
        assertTrue(response is GenericResponse.ErrorResponse)
    }

    @Test
    fun `there is not inventory`() {
        val orderTest = createOrderDTOTest()
        val dropSale = createDropSateTest(productId = orderTest.productId)
        val customerDTO = createCustomerTest()
        orderTest.customer = customerDTO
        val basePrice = dropSale.amount ?: dropSale.product.amount
        Mockito.`when`(dropshippingSaleService.getDropshippingSaleById(orderTest.productId)).thenReturn(listOf(dropSale))
        Mockito.`when`(customerService.createOrReplaceCustomer(customerDTO)).thenReturn(customerDTO.toEntity())
        Mockito.`when`(priceService.getOrderPrice(dropSale.id,orderTest.quantity,basePrice,dropSale.specialConditions)).
        thenReturn(BigDecimal.ZERO)
        Mockito.`when`(inventoryProcessorService.processPrivateInventoryForOrder(customerDTO.city!!,orderTest.quantity,dropSale.product.id!!
                ,dropSale.merchant.id!!,dropSale.product.inventory)).thenReturn(Pair(null,null))
        val response = orderService.createOrder(orderTest)
        assertTrue(response is GenericResponse.ErrorResponse)
    }

    @Test
    fun `MPS payment generation failed`() {
        val orderTest = createOrderDTOTest(paymentMethod = "MPS",orderStatus = 2)
        val customerDTO = createCustomerTest()
        val customer = customerDTO.toEntity()
        val dropSaleObj = createDropSateTest(productId = orderTest.productId)
        val basePrice = dropSaleObj.amount ?: dropSaleObj.product.amount
        orderTest.customer = customerDTO
        Mockito.`when`(dropshippingSaleService.getDropshippingSaleById(orderTest.productId)).thenReturn(listOf(dropSaleObj))
        Mockito.`when`(customerService.createOrReplaceCustomer(customerDTO)).thenReturn(customer)
        Mockito.`when`(priceService.getOrderPrice(dropSaleObj.id,orderTest.quantity,basePrice,dropSaleObj.specialConditions)).
        thenReturn(BigDecimal.ZERO)
        Mockito.`when`(inventoryProcessorService.processPrivateInventoryForOrder(customerDTO.city!!,orderTest.quantity,dropSaleObj.product.id!!
                ,dropSaleObj.merchant.id!!,dropSaleObj.product.inventory)).thenReturn(Pair(12,null))
        Mockito.`when`(productService.createPaymentFromProduct(MockitoHelper.anyObject(),MockitoHelper.anyObject(),MockitoHelper.anyObject()))
                .thenReturn(GenericResponse.ErrorResponse("fail"))
        Mockito.`when`(orderRepository.save(MockitoHelper.anyObject()))
                .thenReturn(orderTest.toEntity(2, createDropSateTest(UUID.randomUUID()),customerId = UUID.randomUUID()))

        val response= orderService.createOrder(orderTest)
        assertTrue(response is GenericResponse.ErrorResponse)
    }

    @Test
    fun `COD payment generation success`() {
        val orderTest = createOrderDTOTest(paymentMethod = "COD",orderStatus = 2)
        val customerDTO = createCustomerTest()
        val customer = customerDTO.toEntity()
        val dropSaleObj = createDropSateTest(productId = orderTest.productId)
        val basePrice = dropSaleObj.amount ?: dropSaleObj.product.amount
        orderTest.customer = customerDTO
        Mockito.`when`(dropshippingSaleService.getDropshippingSaleById(orderTest.productId)).thenReturn(listOf(dropSaleObj))
        Mockito.`when`(customerService.createOrReplaceCustomer(customerDTO)).thenReturn(customer)
        Mockito.`when`(priceService.getOrderPrice(dropSaleObj.id,orderTest.quantity,basePrice,dropSaleObj.specialConditions)).
        thenReturn(BigDecimal.ZERO)
        Mockito.`when`(inventoryProcessorService.processPrivateInventoryForOrder(customerDTO.city!!,orderTest.quantity,dropSaleObj.product.id!!
                ,dropSaleObj.merchant.id!!,dropSaleObj.product.inventory)).thenReturn(Pair(12,null))
        Mockito.`when`(orderRepository.save(MockitoHelper.anyObject()))
                .thenReturn(orderTest.toEntity(2, createDropSateTest(UUID.randomUUID()),customerId = UUID.randomUUID()))

        val response= orderService.createOrder(orderTest)
        assertTrue(response is GenericResponse.SuccessResponse)
    }

    @Test
    fun `MPS payment generation success`() {
        val orderTest = createOrderDTOTest(paymentMethod = "MPS",orderStatus = 2)
        orderTest.paymentId="1234"
        val customerDTO = createCustomerTest()
        val customer = customerDTO.toEntity()
        val dropSaleObj = createDropSateTest(productId = orderTest.productId)
        val basePrice = dropSaleObj.amount ?: dropSaleObj.product.amount
        orderTest.customer = customerDTO
        Mockito.`when`(dropshippingSaleService.getDropshippingSaleById(orderTest.productId)).thenReturn(listOf(dropSaleObj))
        Mockito.`when`(customerService.createOrReplaceCustomer(customerDTO)).thenReturn(customer)
        Mockito.`when`(priceService.getOrderPrice(dropSaleObj.id,orderTest.quantity,basePrice,dropSaleObj.specialConditions)).
        thenReturn(BigDecimal.ZERO)
        Mockito.`when`(inventoryProcessorService.processPrivateInventoryForOrder(customerDTO.city!!,orderTest.quantity,dropSaleObj.product.id!!
                ,dropSaleObj.merchant.id!!,dropSaleObj.product.inventory)).thenReturn(Pair(12,null))
        Mockito.`when`(productService.createPaymentFromProduct(MockitoHelper.anyObject(),MockitoHelper.anyObject(),MockitoHelper.anyObject()))
                .thenReturn(GenericResponse.SuccessResponse("1234"))
        Mockito.`when`(orderRepository.save(MockitoHelper.anyObject()))
                .thenReturn(orderTest.toEntity(2, createDropSateTest(UUID.randomUUID()),customerId = UUID.randomUUID()))

        val response= orderService.createOrder(orderTest)
        assertTrue(response is GenericResponse.SuccessResponse)
    }

    @Test
    fun `no drop products for seller`() {
        val merchantId = UUID.randomUUID()
        Mockito.`when`(dropshippingSaleService.getDropshippingSaleBySellerMerchantId(merchantId)).thenReturn(listOf())
        val response = orderService.getOrdersForDropshipperSeller(merchantId, QueryParams(15, 0))
        assertTrue(response.orders.isEmpty())
        assertTrue(response.totalOrderAmountByStatus == BigDecimal.ZERO)
    }

    @Test
    fun `no drop products for provider`() {
        val merchantId = UUID.randomUUID()
        Mockito.`when`(productService.getDropProductsForMerchant(merchantId)).thenReturn(listOf())
        val response = orderService.getOrdersForProvider(merchantId, QueryParams(15, 0))
        assertTrue(response.orders.isEmpty())
        assertTrue(response.totalOrderAmountByStatus == BigDecimal.ZERO)
    }

    @Test
    fun `there is no orders products for seller`() {
        val checkoutId = UUID.randomUUID()
        val merchantId = UUID.randomUUID()
        val duration = 15
        Mockito.`when`(dropshippingSaleService.getDropshippingSaleBySellerMerchantId(merchantId)).thenReturn(listOf(createProductDropshippingSale(checkoutId)))
        val response = orderService.getOrdersForDropshipperSeller(merchantId, QueryParams(duration, 0))
        assertTrue(response.orders.isEmpty())
        assertTrue(response.totalOrderAmountByStatus == BigDecimal.ZERO)
    }

    @Test
    fun `there is no orders for provider`() {
        val merchantId = UUID.randomUUID()
        Mockito.`when`(productService.getDropProductsForMerchant(merchantId)).thenReturn(listOf(createProductTest()))
        Mockito.`when`(orderRepository.findOrdersByProductsAndtDynamicParams(MockitoHelper.anyObject(),
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject(),
                Mockito.anyInt(),
                MockitoHelper.anyObject(),
                Mockito.anyInt()))
                .thenReturn(OrderRepositoryCustomImpl.PageInformation(listOf(),1,1,1))
        val response = orderService.getOrdersForProvider(merchantId, QueryParams(15, 0))
        assertTrue(response.orders.isEmpty())
        assertTrue(response.totalOrderAmountByStatus == BigDecimal.ZERO)
    }

    @Test
    fun `there is one order for provider, no customer exist`() {
        val productId = UUID.randomUUID()
        val merchantId = UUID.randomUUID()
        val duration = 15
        val customerId = UUID.randomUUID()
        val product = createProductTest(id = productId)
        val productsId = listOf(product)
        val dropProducts = listOf(createDropSateTest(productId))
        val order = createGeneralOrderTest(customerId = customerId, dropSaleId = dropProducts[0].id,productId = productId)
        Mockito.`when`(productService.getDropProductsForMerchant(merchantId)).thenReturn(productsId)
        Mockito.`when`(dropshippingSaleService.findByProductIdInAndDisabledFalse(listOf(productId))).thenReturn(dropProducts)
        Mockito.`when`(orderRepository.findOrdersByProductsAndtDynamicParams(MockitoHelper.anyObject(),
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject(),
                Mockito.anyInt(),
                MockitoHelper.anyObject(),
                Mockito.anyInt()))
               .thenReturn(OrderRepositoryCustomImpl.PageInformation(listOf(order),1,1,1))
        Mockito.`when`(customerService.getCustomerById(customerId))
                .thenReturn(Optional.empty())
        val response = orderService.getOrdersForProvider(merchantId, QueryParams(duration, 0))
        assertTrue(response.orders.size == 1)
        assertTrue(response.orders[0].customerName.isBlank())
        assertTrue(response.totalOrderAmountByStatus == (product.dropshippingPrice?:BigDecimal.ZERO)*BigDecimal(order.quantity))
    }

    @Test
    fun `there is one order for drop products for seller, no customer exist`() {
        val checkoutId = UUID.randomUUID()
        val merchantId = UUID.randomUUID()
        val duration = 15
        val customerId = UUID.randomUUID()
        val order = createGeneralOrderTest(customerId = customerId, dropSaleId = checkoutId)

        Mockito.`when`(dropshippingSaleService.getDropshippingSaleBySellerMerchantId(merchantId,true)).thenReturn(listOf(createProductDropshippingSale(checkoutId)))
        Mockito.`when`(orderRepository.findOrdersByProductsAndtDynamicParams(MockitoHelper.anyObject(),
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject(),
                Mockito.anyInt(),
                MockitoHelper.anyObject(),
                Mockito.anyInt()))
                .thenReturn(OrderRepositoryCustomImpl.PageInformation(listOf(order),1,1,1))
        Mockito.`when`(customerService.getCustomerById(customerId))
                .thenReturn(Optional.empty())
        val response = orderService.getOrdersForDropshipperSeller(merchantId, QueryParams(duration, 0))
        assertTrue(response.orders.size == 1)
        assertTrue(response.orders[0].customerName.isBlank())
        assertTrue(response.totalOrderAmountByStatus == order.amount)
    }

    @Test
    fun `there is one order for drop products for seller, customer exist`() {
        val checkoutId = UUID.randomUUID()
        val merchantId = UUID.randomUUID()
        val duration = 15
        val customerId = UUID.randomUUID()
        val order = createGeneralOrderTest(customerId = customerId, dropSaleId = checkoutId)
        val customer = createCustomerTest()

        Mockito.`when`(dropshippingSaleService.getDropshippingSaleBySellerMerchantId(merchantId,true)).thenReturn(listOf(createProductDropshippingSale(checkoutId)))
        Mockito.`when`(orderRepository.findOrdersByProductsAndtDynamicParams(MockitoHelper.anyObject(),
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject(),
                Mockito.anyInt(),
                MockitoHelper.anyObject(),
                Mockito.anyInt()))
                .thenReturn(OrderRepositoryCustomImpl.PageInformation(listOf(order),1,1,1))
        Mockito.`when`(customerService.getCustomerById(customerId))
                .thenReturn(Optional.of(customer.toEntity()))
        val response = orderService.getOrdersForDropshipperSeller(merchantId, QueryParams(duration, 0))
        assertTrue(response.orders.size == 1)
        assertTrue(response.orders[0].customerName == customer.name)
        assertTrue(response.totalOrderAmountByStatus == order.amount)
    }

    @Test
    fun `there is one order for provider, customer exist`() {
        val productId = UUID.randomUUID()
        val merchantId = UUID.randomUUID()
        val duration = 15
        val customerId = UUID.randomUUID()
        val customer = createCustomerTest()
        val product = createProductTest(id = productId)
        val dropProducts = listOf(createDropSateTest(productId))
        val order = createGeneralOrderTest(customerId = customerId, dropSaleId = dropProducts[0].id,productId = productId)

        Mockito.`when`(productService.getDropProductsForMerchant(merchantId)).thenReturn(listOf(product))
        Mockito.`when`(dropshippingSaleService.findByProductIdInAndDisabledFalse(listOf(productId))).thenReturn(dropProducts)
        Mockito.`when`(orderRepository.findOrdersByProductsAndtDynamicParams(MockitoHelper.anyObject(),
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject(),
                MockitoHelper.anyObject(),
                Mockito.anyInt(),
                MockitoHelper.anyObject(),
                Mockito.anyInt()))
                .thenReturn(OrderRepositoryCustomImpl.PageInformation(listOf(order),1,1,1))
        Mockito.`when`(customerService.getCustomerById(customerId))
                .thenReturn(Optional.of(customer.toEntity()))
        val response = orderService.getOrdersForProvider(merchantId, QueryParams(duration, 0))
        assertTrue(response.orders.size == 1)
        assertTrue(response.orders[0].customerName == customer.name)
        assertTrue(response.totalOrderAmountByStatus == BigDecimal(order.quantity)*(product.dropshippingPrice?: BigDecimal.ZERO))
    }

    @Test
    fun `update order for unexisting order`() {
        val orderId = UUID.randomUUID()
        Mockito.`when`(orderRepository.findById(orderId)).thenReturn(Optional.empty())
        val request = createUpdateOrderRequest(orderId)
        val response = orderService.updateOrder(request)
        assertNull(response)
    }

    @Test
    fun `update order for order in a different state of to be confirmed`() {
        val orderId = UUID.randomUUID()
        Mockito.`when`(orderRepository.findById(orderId)).thenReturn(Optional.of(createOrderDTOTest()
                .toEntity(customerId = UUID.randomUUID(),dropShippingSale = createDropSateTest())))
        val request = createUpdateOrderRequest(orderId)
        val response = orderService.updateOrder(request)
        assertNull(response)
    }

    @Test
    fun `update order for existing order and replacing customer`() {
        val orderId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val order = createOrderDTOTest()
                .toEntity(customerId = customerId,dropShippingSale = createDropSateTest())
        order.orderStatus=10
        Mockito.`when`(orderRepository.findById(orderId)).thenReturn(Optional.of(order))
        val customer = createCustomerTest(id=customerId).toEntity()
        Mockito.`when`(customerService.createOrReplaceCustomer(MockitoHelper.anyObject())).thenReturn(customer)
        val request = createUpdateOrderRequest(orderId)
        val response = orderService.updateOrder(request)
        assertEquals(response,customer.toDTO())
    }

    @Test
    fun `update order for existing order and creating customer`() {
        val orderId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val customerIdNew = UUID.randomUUID()
        val order = createOrderDTOTest()
                .toEntity(customerId = customerId,dropShippingSale = createDropSateTest())
        order.orderStatus = 10
        Mockito.`when`(orderRepository.findById(orderId)).thenReturn(Optional.of(order))
        val customer = createCustomerTest(id=customerIdNew).toEntity()
        Mockito.`when`(customerService.createOrReplaceCustomer(MockitoHelper.anyObject())).thenReturn(customer)
        Mockito.`when`(orderRepository.save(MockitoHelper.anyObject())).thenReturn(order)
        val request = createUpdateOrderRequest(orderId)
        val response = orderService.updateOrder(request)
        assertEquals(response?.id,customerIdNew)
    }
}
