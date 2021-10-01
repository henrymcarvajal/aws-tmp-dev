package com.mps.payment.core.service.processor

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.model.toEntity
import com.mps.payment.core.repository.OrderRepository
import com.mps.payment.core.service.*
import com.mps.payment.core.util.createCustomerTest
import com.mps.payment.core.util.createDropSateTest
import com.mps.payment.core.util.createOrderDTOTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.math.BigDecimal

internal class OrderProcessorTemplateTest {

    private lateinit var orderProcessorTemplate: OrderProcessorTemplate

    @Mock
    private lateinit var productServiceMock: ProductService

    @Mock
    private lateinit var customerService: CustomerService

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var inventoryProcessorService: InventoryProcessorService

    @Mock
    private lateinit var dropshippingSaleService: DropshippingSaleService

    @Mock
    private lateinit var priceService: PriceService

    @BeforeEach
    fun setup(){
        MockitoAnnotations.initMocks(this)
        orderProcessorTemplate = MPSProcessor(productServiceMock)
        orderProcessorTemplate.setDependencies(
                customerService=customerService,orderRepository=orderRepository,inventoryProcessorService,dropshippingSaleService,
                priceService
        )
    }

    @Test
    fun `product does not exist`() {
        val orderTest = createOrderDTOTest()
        Mockito.`when`(dropshippingSaleService.getDropshippingSaleById(orderTest.productId)).thenReturn(listOf())
        val response = orderProcessorTemplate.preProcessOrder(orderTest)
        Assertions.assertTrue(response is GenericResponse.ErrorResponse)
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
        val response = orderProcessorTemplate.preProcessOrder(orderTest)
        Assertions.assertTrue(response is GenericResponse.ErrorResponse)
    }
}