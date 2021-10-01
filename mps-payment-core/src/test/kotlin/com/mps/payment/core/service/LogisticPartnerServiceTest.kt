package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.client.entregalo.EntregaloClient
import com.mps.payment.core.client.entregalo.EntregaloData
import com.mps.payment.core.client.entregalo.payload.CityDTO
import com.mps.payment.core.model.toEntity
import com.mps.payment.core.repository.CityRepository
import com.mps.payment.core.repository.OrderRepository
import com.mps.payment.core.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.*

internal class LogisticPartnerServiceTest {

    @Mock
    private lateinit var paymentService: PaymentService

    @Mock
    private lateinit var communicationService: CommunicationService

    @Mock
    private lateinit var orderService: OrderService

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var customerService: CustomerService

    @Mock
    private lateinit var entregaloClient: EntregaloClient

    @Mock
    private lateinit var cityRepository: CityRepository

    @Mock
    private lateinit var productService: ProductService

    @Mock
    private lateinit var merchantService: MerchantService

    private lateinit var logisticPartnerService: LogisticPartnerService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
        logisticPartnerService = LogisticPartnerService(customerService = customerService, paymentService = paymentService,
                entregaloClient = entregaloClient, communicationService = communicationService,
                cityRepository = cityRepository, orderRepository = orderRepository)
    }

    val updateFunction = { paymentId: String, guide: Int,label:String -> orderService.updateGuideToOrder(paymentId, guide,label) }

    @Test
    fun `MPS payment does not exist`() {
        val paymentId = "123456"
        Mockito.`when`(paymentService.getPaymentByShortId(paymentId)).thenReturn(listOf())
        val response = logisticPartnerService.requestFreightMPS(paymentId, updateFunction)
        assertTrue(response is GenericResponse.ErrorResponse)
        assertEquals("Payment does not exist", response.objP as String)
    }

    @Test
    fun `MPS merchant does not exist`() {
        val paymentId = "123456"
        val payment = createPaymentTest().toEntity()
        Mockito.`when`(paymentService.getPaymentByShortId(paymentId)).thenReturn(listOf(payment))
        Mockito.`when`(orderRepository.getByPaymentId(paymentId)).thenReturn(Optional.empty())
        val response = logisticPartnerService.requestFreightMPS(paymentId, updateFunction)
        assertTrue(response is GenericResponse.ErrorResponse)
        assertEquals("No hay orden asociada al pago", response.objP as String)
    }

    @Test
    fun `MPS payment does not have customer`() {
        val paymentId = "123456"
        val payment = createPaymentTest().toEntity()
        payment.idCustomer = null
        Mockito.`when`(paymentService.getPaymentByShortId(paymentId)).thenReturn(listOf(payment))
        Mockito.`when`(orderRepository.getByPaymentId(paymentId)).thenReturn(Optional.of(createGeneralOrderTest(customerId = UUID.randomUUID())))
        val response = logisticPartnerService.requestFreightMPS(paymentId, updateFunction)
        assertTrue(response is GenericResponse.ErrorResponse)
        assertEquals("Payment does not have customer yet", response.objP as String)
    }

    @Test
    fun `MPS customer does not exist`() {
        val paymentId = "123456"
        val payment = createPaymentTest().toEntity()
        Mockito.`when`(paymentService.getPaymentByShortId(paymentId)).thenReturn(listOf(payment))
        Mockito.`when`(orderRepository.getByPaymentId(paymentId)).thenReturn(Optional.of(createGeneralOrderTest(customerId = UUID.randomUUID())))
        Mockito.`when`(customerService.getCustomerById(payment.idCustomer!!)).thenReturn(Optional.empty())
        val response = logisticPartnerService.requestFreightMPS(paymentId, updateFunction)
        assertTrue(response is GenericResponse.ErrorResponse)
        assertEquals("customer does not exist", response.objP as String)
    }

    @Test
    fun `MPS happy path`() {
        val paymentId = "123456"
        val payment = createPaymentTest().toEntity()
        Mockito.`when`(paymentService.getPaymentByShortId(paymentId)).thenReturn(listOf(payment))
        Mockito.`when`(orderRepository.getByPaymentId(paymentId)).thenReturn(Optional.of(createGeneralOrderTest(customerId = UUID.randomUUID())))
        Mockito.`when`(customerService.getCustomerById(payment.idCustomer!!)).thenReturn(Optional.of(createCustomerTest().toEntity()))
        Mockito.`when`(entregaloClient.sendFreightRequest(MockitoHelper.anyObject())).thenReturn(createAskNewServiceResponseSuccessful())
        val response = logisticPartnerService.requestFreightMPS(paymentId, updateFunction)
        val responseObj= response.objP as EntregaloData
        assertTrue(response is GenericResponse.SuccessResponse)
        assertEquals("1234", responseObj.Guia)
        assertEquals("label", responseObj.Etiqueta)
    }

    @Test
    fun `MPS error calling logistic partner`() {
        val paymentId = "123456"
        val payment = createPaymentTest().toEntity()
        Mockito.`when`(paymentService.getPaymentByShortId(paymentId)).thenReturn(listOf(payment))
        Mockito.`when`(orderRepository.getByPaymentId(paymentId)).thenReturn(Optional.of(createGeneralOrderTest(customerId = UUID.randomUUID())))
        Mockito.`when`(customerService.getCustomerById(payment.idCustomer!!)).thenReturn(Optional.of(createCustomerTest().toEntity()))
        Mockito.`when`(entregaloClient.sendFreightRequest(MockitoHelper.anyObject())).thenReturn(createAskNewServiceResponseFail())
        val response = logisticPartnerService.requestFreightMPS(paymentId, updateFunction)
        assertTrue(response is GenericResponse.ErrorResponse)
        assertEquals("Error processing logistic", response.objP as String)
    }

    @Test
    fun `COD call logistic partner fail`() {
        val merchant = createMerchantTest().toEntity()
        Mockito.`when`(entregaloClient.sendFreightRequest(MockitoHelper.anyObject())).thenReturn(createAskNewServiceResponseFail())
        Mockito.`when`(merchantService.findById(MockitoHelper.anyObject()))
                .thenReturn(Optional.of(createMerchantTest(UUID.randomUUID()).toEntity()))
        val response = logisticPartnerService.requestFreightCOD(merchant, createCustomerTest().toEntity(), createOrderDTOTest(),
                "productName")
        assertTrue(response is GenericResponse.ErrorResponse)
        assertEquals("Error calling Entregalo", response.objP as String)
    }

    @Test
    fun `COD call logistic partner successful`() {
        val merchant = createMerchantTest().toEntity()
        Mockito.`when`(entregaloClient.sendFreightRequest(MockitoHelper.anyObject())).thenReturn(createAskNewServiceResponseSuccessful())
        Mockito.`when`(merchantService.findById(MockitoHelper.anyObject()))
                .thenReturn(Optional.of(createMerchantTest(UUID.randomUUID()).toEntity()))
        val response = logisticPartnerService.requestFreightCOD(merchant, createCustomerTest().toEntity(), createOrderDTOTest(),
                "productName")
        val responseObj= response.objP as EntregaloData
        assertTrue(response is GenericResponse.SuccessResponse)
        assertEquals("1234", responseObj.Guia)
        assertEquals("label", responseObj.Etiqueta)
    }

    @Test
    fun `MPS getCities succesful from partner and there are records in our database`() {
        val citiesResponse = listOf(createCity())
        Mockito.`when`(entregaloClient.getCities()).thenReturn(citiesResponse)
        Mockito.`when`(cityRepository.count()).thenReturn(100L)
        val response = logisticPartnerService.getCities()
        assertEquals(citiesResponse[0].state, (response as List<CityDTO>)[0].state)
    }

    @Test
    fun `MPS getCities fail from partner and fetch from database succesfully`() {
        val cities = listOf(createCity())
        Mockito.`when`(entregaloClient.getCities()).thenReturn(null)
        Mockito.`when`(cityRepository.findAll()).thenReturn(cities.map { city -> city.toEntity() })
        val response = logisticPartnerService.getCities()
        assertEquals(cities[0].state, (response as List<CityDTO>)[0].state)
    }

    @Test
    fun `MPS getCities fail from partner and fetch from database Fail`() {
        Mockito.`when`(entregaloClient.getCities()).thenReturn(null)
        Mockito.`when`(cityRepository.findAll()).thenReturn(listOf())
        val response = logisticPartnerService.getCities()
        assertNull(response)
    }

    @Test
    fun `external freight request order does not exist`() {
        val orderId = UUID.randomUUID()
        Mockito.`when`(orderRepository.findById(orderId)).thenReturn(Optional.empty())
        val response = logisticPartnerService.externalRequestFreightCOD(orderId, UUID.randomUUID())
        assertTrue(response is GenericResponse.ErrorResponse)
    }

    @Test
    fun `external freight request customer does not exist`() {
        val orderId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        Mockito.`when`(orderRepository.findById(orderId)).thenReturn(Optional.of(createGeneralOrderTest(customerId)))
        Mockito.`when`(customerService.getCustomerById(customerId)).thenReturn(Optional.empty())
        val response = logisticPartnerService.externalRequestFreightCOD(orderId, UUID.randomUUID())
        assertTrue(response is GenericResponse.ErrorResponse)
    }

    @Test
    fun `external freight unauthorized merchant confirming`() {
        val orderId = UUID.randomUUID()
        val order = createGeneralOrderTest(UUID.randomUUID())
        Mockito.`when`(orderRepository.findById(orderId)).thenReturn(Optional.of(order))
        val response = logisticPartnerService.externalRequestFreightCOD(orderId, UUID.randomUUID())
        assertTrue(response is GenericResponse.ErrorResponse)
    }

    @Test
    fun `external freight request dropsale does not exist`() {
        val orderId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val order = createGeneralOrderTest(customerId)
        Mockito.`when`(orderRepository.findById(orderId)).thenReturn(Optional.of(order))
        Mockito.`when`(customerService.getCustomerById(customerId)).thenReturn(Optional.of(createCustomerTest().toEntity()))
        val response = logisticPartnerService.externalRequestFreightCOD(orderId, UUID.randomUUID())
        assertTrue(response is GenericResponse.ErrorResponse)
    }

    @Test
    fun `external freight request product does not exist`() {
        val orderId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val order = createGeneralOrderTest(customerId)
        val productId = UUID.randomUUID()
        Mockito.`when`(orderRepository.findById(orderId)).thenReturn(Optional.of(order))
        Mockito.`when`(customerService.getCustomerById(customerId)).thenReturn(Optional.of(createCustomerTest().toEntity()))
        Mockito.`when`(productService.findById(productId)).thenReturn(Optional.empty())
        val response = logisticPartnerService.externalRequestFreightCOD(orderId, UUID.randomUUID())
        assertTrue(response is GenericResponse.ErrorResponse)
    }

    @Test
    fun `external freight request successful`() {
        val orderId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val order = createGeneralOrderTest(customerId)
        val productId = UUID.randomUUID()
        Mockito.`when`(orderRepository.findById(orderId)).thenReturn(Optional.of(order))
        Mockito.`when`(customerService.getCustomerById(customerId)).thenReturn(Optional.of(createCustomerTest().toEntity()))
        Mockito.`when`(productService.findById(productId)).thenReturn(Optional.of(createProductTest()))
        Mockito.`when`(entregaloClient.sendFreightRequest(MockitoHelper.anyObject())).thenReturn(createAskNewServiceResponseSuccessful())
        Mockito.`when`(merchantService.findById(MockitoHelper.anyObject()))
                .thenReturn(Optional.of(createMerchantTest(UUID.randomUUID()).toEntity()))
        val response = logisticPartnerService.externalRequestFreightCOD(orderId, order.dropShippingSale.merchant.id!!)
        assertTrue(response is GenericResponse.SuccessResponse)
        assertEquals(1234, response.objP as Int)
    }

}