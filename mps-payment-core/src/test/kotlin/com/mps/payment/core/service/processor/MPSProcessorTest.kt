package com.mps.payment.core.service.processor

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.enum.OrderStatus
import com.mps.payment.core.model.toEntity
import com.mps.payment.core.repository.OrderRepository
import com.mps.payment.core.service.*
import com.mps.payment.core.util.*
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

internal class MPSProcessorTest {

    private lateinit var mpsProcessor: OrderProcessorTemplate

    @Mock
    private lateinit var productServiceMock: ProductService

    @BeforeEach
    fun setup(){
        MockitoAnnotations.initMocks(this)
        mpsProcessor = MPSProcessor(productServiceMock)
    }

    @Test
    fun `MPS payment generation failed`() {
       val order = createOrderDTOTest()
        val customer = createCustomerTest().toEntity()
        val product = createProductTest()
        val dropSale = createDropSateTest()
        dropSale.product = product
        val sellerMerchant = createMerchantTest().toEntity()
        val entity = order.toEntity(dropShippingSale = dropSale,customerId = customer.id!!)
        Mockito.`when`(productServiceMock.createPaymentFromProduct(MockitoHelper.anyObject(),MockitoHelper.anyObject(),
                MockitoHelper.anyObject())).thenReturn(GenericResponse.ErrorResponse("Error generating payment"))
        mpsProcessor.process(orderDropDTO = order,customer = customer,product = product,generalOrderDropEntity = entity,
                sellerMerchant = sellerMerchant)
        assertTrue(entity.orderStatus == OrderStatus.FAILED.state)
    }

    @Test
    fun `MPS payment generation success`() {
        val order = createOrderDTOTest()
        val customer = createCustomerTest().toEntity()
        val product = createProductTest()
        val dropSale = createDropSateTest()
        dropSale.product = product
        val sellerMerchant = createMerchantTest().toEntity()
        val entity = order.toEntity(dropShippingSale = dropSale,customerId = customer.id!!)
        Mockito.`when`(productServiceMock.createPaymentFromProduct(MockitoHelper.anyObject(),MockitoHelper.anyObject(),
                MockitoHelper.anyObject())).thenReturn(GenericResponse.SuccessResponse("123456"))
        mpsProcessor.process(orderDropDTO = order,customer = customer,product = product,generalOrderDropEntity = entity,
                sellerMerchant = sellerMerchant)
        assertTrue(entity.orderStatus == OrderStatus.PAYMENT_PENDING.state)
    }
}