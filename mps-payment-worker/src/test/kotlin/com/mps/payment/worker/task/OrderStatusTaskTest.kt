package com.mps.payment.worker.task

import com.mps.payment.core.client.entregalo.EntregaloClient
import com.mps.payment.core.email.EmailSender
import com.mps.payment.core.enum.OrderStatus
import com.mps.payment.core.model.GeneralOrderDrop
import com.mps.payment.core.model.InternalNumbers
import com.mps.payment.core.model.toEntity
import com.mps.payment.core.repository.InternalNumberRepository
import com.mps.payment.core.repository.OrderRepository
import com.mps.payment.worker.util.*
import com.mps.payment.worker.util.MockitoHelper.anyObject
import org.slf4j.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.*
import org.mockito.internal.verification.AtLeast
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.util.*


internal class OrderStatusTaskTest {

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var internalNumberRepository: InternalNumberRepository

    @Mock
    private lateinit var entregaloClient: EntregaloClient

    @Mock
    private lateinit var orderStatusTask: OrderStatusTask

    @Mock
    private lateinit var emailSender: EmailSender

    @Mock
    private lateinit var log: Logger

    @Captor
    private lateinit var orderCaptor: ArgumentCaptor<GeneralOrderDrop>

    @Captor
    private lateinit var internalNumbersCaptor: ArgumentCaptor<InternalNumbers>

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        orderStatusTask = OrderStatusTask(orderRepository = orderRepository, entregaloClient = entregaloClient,
            internalNumberRepository = internalNumberRepository,emailSender = emailSender)
        orderStatusTask.log = log
    }

    @Test
    fun `No orders available to update`() {
        Mockito.`when`(orderRepository.findByOrderStatusIsNotAndGuideNumberIsNotNull(OrderStatus.DELIVERED.state))
            .thenReturn(listOf())
        orderStatusTask.run()
        Mockito.verify(orderStatusTask.log).info("There is not record without FreightPrice")
    }

    @Test
    fun `Successfully order update`() {
        var orderTest = createOrderDTOTest(guideNumber = 300)
            .toEntity(dropShippingSale = createDropSateTest(UUID.randomUUID()),customerId = UUID.randomUUID())
        orderTest.customerId = UUID.randomUUID()
        Mockito.`when`(orderRepository.findByOrderStatusIsNotAndGuideNumberIsNotNull(OrderStatus.DELIVERED.state))
            .thenReturn(listOf(orderTest))
        Mockito.`when`(entregaloClient.sendQueryServiceStatusRequest(MockitoHelper.anyObject())).thenReturn(
            createQueryServiceStatusResponse(HttpStatus.CREATED)
        )
        Mockito.`when`(orderRepository.save(orderTest)).thenReturn(orderTest)

        orderStatusTask.run()

        Mockito.verify(orderRepository, AtLeast(1)).save(orderTest)
    }

    @Test
    fun `No successfully order update`() {
        var orderTest = createOrderDTOTest()
            .toEntity(dropShippingSale = createDropSateTest(UUID.randomUUID()),customerId = UUID.randomUUID())
        orderTest.guideNumber = 300
        Mockito.`when`(orderRepository.findByOrderStatusIsNotAndGuideNumberIsNotNull(OrderStatus.DELIVERED.state)).thenReturn(listOf(orderTest))
        Mockito.`when`(entregaloClient.sendQueryServiceStatusRequest(anyObject())).thenReturn(
            createQueryServiceStatusResponse(HttpStatus.EXPECTATION_FAILED)
        )
        orderStatusTask.run()
        Mockito.verify(orderStatusTask.log, AtLeast(1)).error("Error calling entregalo for updating order ${orderTest.id}")
    }

    @Test
    fun `Successfully order update, internal number does not exist`() {
        var orderTest = createOrderDTOTest(guideNumber = 300)
            .toEntity(dropShippingSale = createDropSateTest(UUID.randomUUID()),customerId = UUID.randomUUID())
        orderTest.customerId = UUID.randomUUID()
        Mockito.`when`(orderRepository.findByOrderStatusIsNotAndGuideNumberIsNotNull(OrderStatus.DELIVERED.state)).thenReturn(listOf(orderTest))
        Mockito.`when`(internalNumberRepository.findByOrderId(MockitoHelper.anyObject())).thenReturn(listOf())
        Mockito.`when`(entregaloClient.sendQueryServiceStatusRequest(MockitoHelper.anyObject())).thenReturn(
            createQueryServiceStatusResponse(HttpStatus.CREATED)
        )
        Mockito.`when`(orderRepository.save(orderTest)).thenReturn(orderTest)

        orderStatusTask.run()

        Mockito.verify(orderRepository, AtLeast(1)).save(orderTest)
        Mockito.verify(log, AtLeast(1)).error(Mockito.anyString())
    }

    @Test
    fun `Successfully order update, internal number exist,non Bogota customer`() {
        var orderTest = createOrderDTOTest(guideNumber = 300)
            .toEntity(dropShippingSale = createDropSateTest(UUID.randomUUID()),customerId = UUID.randomUUID())
        orderTest.customerId = UUID.randomUUID()
        val internalNumberTest = createInternalNumberTest()
        val customer = createCustomerTest()
        val entregaloResponse = createQueryServiceStatusResponse(HttpStatus.CREATED)
        customer.city = "110115000"
        Mockito.`when`(orderRepository.findByOrderStatusIsNotAndGuideNumberIsNotNull(OrderStatus.DELIVERED.state)).thenReturn(listOf(orderTest))
        Mockito.`when`(internalNumberRepository.findByOrderId(MockitoHelper.anyObject())).thenReturn(listOf(internalNumberTest))
        Mockito.`when`(entregaloClient.sendQueryServiceStatusRequest(MockitoHelper.anyObject())).thenReturn(entregaloResponse)
        Mockito.`when`(orderRepository.save(orderTest)).thenReturn(orderTest)
        Mockito.`when`(internalNumberRepository.save(internalNumberTest)).thenReturn(internalNumberTest)
        orderStatusTask.run()

        Mockito.verify(orderRepository).save(orderCaptor.capture())
        Mockito.verify(internalNumberRepository).save(internalNumbersCaptor.capture())
        val order = orderCaptor.value
        assert(order.freightPrice == entregaloResponse.freightPrice)
        val internalNumber = internalNumbersCaptor.value
        assert(internalNumber.freightCommission == entregaloResponse
            .freightPrice!!.multiply(BigDecimal(FREIGHT_NATIONAL_COMMISSION)).divide(BigDecimal(100)))

    }

    @Test
    fun `Successfully order update, freight price is not null, entregalo state is process`() {
        var orderTest = createOrderDTOTest(guideNumber = 300)
            .toEntity(dropShippingSale = createDropSateTest(UUID.randomUUID()),customerId = UUID.randomUUID())
        orderTest.customerId = UUID.randomUUID()
        orderTest.freightPrice= BigDecimal(8000)

        val customer = createCustomerTest()
        val entregaloResponse = createQueryServiceStatusResponse(HttpStatus.CREATED)
        customer.city = "110115000"
        Mockito.`when`(orderRepository.findByOrderStatusIsNotAndGuideNumberIsNotNull(OrderStatus.DELIVERED.state)).thenReturn(listOf(orderTest))
        Mockito.`when`(entregaloClient.sendQueryServiceStatusRequest(MockitoHelper.anyObject())).thenReturn(entregaloResponse)
        Mockito.`when`(orderRepository.save(orderTest)).thenReturn(orderTest)
        orderStatusTask.run()

        Mockito.verify(orderRepository).save(orderCaptor.capture())
        val order = orderCaptor.value
        assert(order.orderStatus == OrderStatus.ON_DELIVERY.state)

    }

    @Test
    fun `Successfully order update, freight price is not null, entregalo state is PendingCheck`() {
        var orderTest = createOrderDTOTest(guideNumber = 300)
            .toEntity(dropShippingSale = createDropSateTest(UUID.randomUUID()),customerId = UUID.randomUUID())
        orderTest.customerId = UUID.randomUUID()
        orderTest.freightPrice=BigDecimal(8000)

        val customer = createCustomerTest()
        val entregaloResponse = createQueryServiceStatusResponse(HttpStatus.CREATED,"PendingCheck")
        customer.city = "110115000"
        Mockito.`when`(orderRepository.findByOrderStatusIsNotAndGuideNumberIsNotNull(OrderStatus.DELIVERED.state)).thenReturn(listOf(orderTest))
        Mockito.`when`(entregaloClient.sendQueryServiceStatusRequest(MockitoHelper.anyObject())).thenReturn(entregaloResponse)
        Mockito.`when`(orderRepository.save(orderTest)).thenReturn(orderTest)
        orderStatusTask.run()

        Mockito.verify(orderRepository).save(orderCaptor.capture())
        val order = orderCaptor.value
        assert(order.orderStatus == OrderStatus.TO_DISPATCH.state)

    }

    @Test
    fun `Successfully order update, freight price is not null, entregalo state is Return`() {
        var orderTest = createOrderDTOTest(guideNumber = 300)
            .toEntity(dropShippingSale = createDropSateTest(UUID.randomUUID()),customerId = UUID.randomUUID())
        orderTest.customerId = UUID.randomUUID()
        orderTest.freightPrice=BigDecimal(8000)

        val customer = createCustomerTest()
        val entregaloResponse = createQueryServiceStatusResponse(HttpStatus.CREATED,"Return")
        customer.city = "110115000"
        Mockito.`when`(orderRepository.findByOrderStatusIsNotAndGuideNumberIsNotNull(OrderStatus.DELIVERED.state)).thenReturn(listOf(orderTest))
        Mockito.`when`(entregaloClient.sendQueryServiceStatusRequest(MockitoHelper.anyObject())).thenReturn(entregaloResponse)
        Mockito.`when`(orderRepository.save(orderTest)).thenReturn(orderTest)
        orderStatusTask.run()

        Mockito.verify(orderRepository).save(orderCaptor.capture())
        val order = orderCaptor.value
        assert(order.orderStatus == OrderStatus.RETURN.state)

    }
}