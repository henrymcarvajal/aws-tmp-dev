package com.mps.payment.core.service.processor

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.enum.OrderStatus
import com.mps.payment.core.model.toEntity
import com.mps.payment.core.service.LogisticPartnerService
import com.mps.payment.core.service.ProductService
import com.mps.payment.core.util.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.BeforeEach
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

internal class WarrantyProcessorTest {

    private lateinit var warrantyProcessor: OrderProcessorTemplate

    @Mock
    private lateinit var logisticPartnerService: LogisticPartnerService

    @BeforeEach
    fun setup(){
        MockitoAnnotations.initMocks(this)
        warrantyProcessor = WarrantyProcessor(logisticPartnerService)
    }


    @Test
    fun `Freight Request failed`() {
        val order = createOrderDTOTest()
        val customer = createCustomerTest().toEntity()
        val product = createProductTest()
        val dropSale = createDropSateTest()
        dropSale.product = product
        val sellerMerchant = createMerchantTest().toEntity()
        val entity = order.toEntity(dropShippingSale = dropSale,customerId = customer.id!!)
        Mockito.`when`(logisticPartnerService.requestFreightCOD(sellerMerchant,customer,
                order,product.name!!,true))
                .thenReturn(GenericResponse.ErrorResponse("Error generating freight"))
        warrantyProcessor.process(orderDropDTO = order,customer = customer,product = product,generalOrderDropEntity = entity,
                sellerMerchant = sellerMerchant)
        Assertions.assertTrue(entity.orderStatus == OrderStatus.TO_DISPATCH.state)
    }

    @Test
    fun `Freight Request success`() {
        val order = createOrderDTOTest()
        val customer = createCustomerTest().toEntity()
        val product = createProductTest()
        val dropSale = createDropSateTest()
        val guideNumber ="123456"
        dropSale.product = product
        val sellerMerchant = createMerchantTest().toEntity()
        val entity = order.toEntity(dropShippingSale = dropSale,customerId = customer.id!!)
        Mockito.`when`(logisticPartnerService.requestFreightCOD(sellerMerchant,customer,
                order,product.name!!,true))
                .thenReturn(GenericResponse.SuccessResponse(guideNumber))
        warrantyProcessor.process(orderDropDTO = order,customer = customer,product = product,generalOrderDropEntity = entity,
                sellerMerchant = sellerMerchant)
        Assertions.assertTrue(entity.guideNumber.toString() == guideNumber)
    }
}