package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.OrderDropDTO
import com.mps.payment.core.client.entregalo.EntregaloClient
import com.mps.payment.core.client.entregalo.EntregaloData
import com.mps.payment.core.client.entregalo.payload.AskNewServiceRequestInput
import com.mps.payment.core.client.entregalo.payload.AskNewServiceResponse
import com.mps.payment.core.client.entregalo.payload.CityDTO
import com.mps.payment.core.enum.OrderStatus
import com.mps.payment.core.model.*
import com.mps.payment.core.repository.CityRepository
import com.mps.payment.core.repository.OrderRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

@Service
class LogisticPartnerService(private val paymentService: PaymentService,
                             private val customerService: CustomerService,
                             private val entregaloClient: EntregaloClient,
                             private val communicationService: CommunicationService,
                             private val orderRepository: OrderRepository,
                             private val cityRepository: CityRepository
) {

    @Value("\${entregalo.api.url}")
    var apiURL: String = "https://test.com"
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun requestFreightMPS(paymentID: String, update: (String, Int,String) -> OrderDropDTO?): GenericResponse<*> {
        return try {
            val paymentList = paymentService.getPaymentByShortId(paymentID)
            if (paymentList.isEmpty()) {
                return GenericResponse.ErrorResponse("Payment does not exist")
            }
            val payment = paymentList[0]

            val order = orderRepository.getByPaymentId(paymentID)
            if(order.isEmpty){
                log.error("There is no order associated to that payment")
                return GenericResponse.ErrorResponse("No hay orden asociada al pago")
            }
            val orderValue= order.get()

            val productName= getProductNameAndUpdatePayment(orderValue, payment)
            if (orderValue.branchCode==0) {
                log.error("branch code is empty")
                communicationService.sendEmailWithTemplate(receiver = "operativo@mipagoseguro.co", templateName = TEMPLATE_EMAIL_PLANE_TEXT,
                        title = "Error generando guia a orden",
                        o = mapOf(CONST_MESSAGE to
                                "Orden: orderId: ${orderValue.id}, valor de la compra : ${orderValue.amount}, paymentMethod: " +
                                "${orderValue.paymentMethod}",
                                CONST_TITLE_BODY to "Es necesario revisar logs y procesar guia manual"))
                GenericResponse.ErrorResponse("Error calling Entregalo")
                return GenericResponse.ErrorResponse("branch code es vacio")
            }
            val customer = payment
                    .idCustomer?.let {
                        val customerOptional = customerService.getCustomerById(it)
                        if (customerOptional.isEmpty) {
                            return GenericResponse.ErrorResponse("customer does not exist")
                        } else {
                            customerOptional.get()
                        }
                    } ?: return GenericResponse
                    .ErrorResponse("Payment does not have customer yet")
            log.info("customer ${customer}")
            val shotProductName = productName?.let {cutProductName(productName)}?:"Sin nombre"
            val entregaloRequest = AskNewServiceRequestInput(address = customer.address!!,
                    amountToReceive = BigDecimal.ZERO, declaredValue = payment.amount, peopleCity = customer.city!!,
                    peopleState = customer.department!!, peopleReceiver = "${customer.name}",
                    peoplePhone = customer.contactNumber.toLong(), branch = orderValue.branchCode,
                    deliverySector = customer.neighborhood!!,
                    order = orderValue.id.toString().takeLast(6),observation = "$shotProductName cantidad: ${orderValue.quantity}")
            val entregaloResponse = entregaloClient.sendFreightRequest(entregaloRequest)
            entregaloResponse?.let { update(paymentID, Integer.valueOf(entregaloResponse.guia),entregaloResponse.label) }
            processEntregaloResponse(entregaloResponse, customer, payment.amount, "MPS")
        } catch (e: Exception) {
            log.error("Error processing logistic, request freight MPS", e)
            GenericResponse.ErrorResponse("Error processing logistic")
        }
    }

    private fun getProductNameAndUpdatePayment(orderValue: GeneralOrderDrop, payment: Payment):String {
                val dropSaleValue = orderValue.dropShippingSale
                payment.idMerchant = dropSaleValue.merchant.id!!
                paymentService.save(payment)
                return dropSaleValue.product.name?:"Nombre vacio"
    }

    fun externalRequestFreightCOD(orderId:UUID,sellerMerchantId:UUID):GenericResponse<*>{
        val optionalOrder = orderRepository.findById(orderId)
        if(optionalOrder.isEmpty){
            return GenericResponse.ErrorResponse("Orden no existe")
        }
        val order = optionalOrder.get()
        if(order.guideNumber!=null){
            return GenericResponse.SuccessResponse(order.guideNumber)
        }
        if(sellerMerchantId != order.dropShippingSale.merchant.id){
            return GenericResponse.ErrorResponse("No esta autorizado para confirmar la orden.")
        }
        return generateFreightByOrder(order)
    }

    fun generateFreightByOrder(order: GeneralOrderDrop, customer: Customer? = null): GenericResponse<out Any?> {
        val finalCustomer = if (customer == null) {
            val optionalCustomer = customerService.getCustomerById(order.customerId!!)
            if (optionalCustomer.isEmpty) {
                return GenericResponse.ErrorResponse("No existe cliente para esa orden")
            }
            optionalCustomer.get()
        } else {
            customer
        }
        val product = order.dropShippingSale.product
        return when (val response = requestFreightCOD(merchant = order.dropShippingSale.merchant, customer = finalCustomer, orderDropDTO = order.toDTO(),
                productName = product.name ?: "nombre vacio")) {
            is GenericResponse.SuccessResponse -> {
                 val response = (response.obj as EntregaloData)
                order.guideNumber = response.Guia.toInt()
                order.label= response.Etiqueta
                order.orderStatus = OrderStatus.TO_DISPATCH.state
                orderRepository.save(order)
                GenericResponse.SuccessResponse(order)
            }
            is GenericResponse.ErrorResponse -> {
                response
            }
        }
    }

    fun requestFreightCOD(merchant: Merchant, customer: Customer, orderDropDTO: OrderDropDTO, productName:String, isOnline:Boolean=false): GenericResponse<*> {
        return try {
            if (orderDropDTO.branchCode==0) {
                communicationService.sendEmailWithTemplate(receiver = "operativo@mipagoseguro.co", templateName = TEMPLATE_EMAIL_PLANE_TEXT,
                        title = "Error generando guia a orden",
                        o = mapOf(CONST_MESSAGE to
                                "Orden: customerId: ${customer.id}, valor de la compra : ${orderDropDTO.amount}, paymentMethod: COD",
                                CONST_TITLE_BODY to "Es necesario revisar logs y procesar guia manual"))
                GenericResponse.ErrorResponse("Error branch code is null")
            } else {
                val shortProductName = cutProductName(productName)
                val merchantName = merchant.name
                val entregaloRequest = if(isOnline){
                    AskNewServiceRequestInput(address = customer.address!!,
                            amountToReceive = BigDecimal.ZERO, declaredValue = orderDropDTO.amount, peopleCity = customer.city!!,
                            peopleState = customer.department!!, peopleReceiver = "${customer.name}",
                            peoplePhone = customer.contactNumber.toLong(), branch = orderDropDTO.branchCode,
                            deliverySector = customer.neighborhood!!,
                            order = "Producto:${shortProductName} Cantidad:${orderDropDTO.quantity}", observation = "Producto:$shortProductName Tienda:${merchantName}")
                }else{
                    AskNewServiceRequestInput(address = customer.address!!,
                            amountToReceive = orderDropDTO.amount, declaredValue = orderDropDTO.amount, peopleCity = customer.city!!,
                            peopleState = customer.department!!, peopleReceiver = customer.name,
                            peoplePhone = customer.contactNumber.toLong(), branch = orderDropDTO.branchCode, deliverySector = customer.neighborhood!!,
                            order = "Producto:$shortProductName Cantidad:${orderDropDTO.quantity}", observation =  "Producto:$shortProductName Tienda:${merchantName}")
                }

                val entregaloResponse = entregaloClient.sendFreightRequest(entregaloRequest)
                processEntregaloResponse(entregaloResponse, customer, orderDropDTO.amount, "COD")
            }
        } catch (e: Exception) {
            log.error("Error processing logistic, request freight MPS", e)
            GenericResponse.ErrorResponse("Error processing logistic")
        }
    }

    private fun cutProductName(productName: String) = if (productName.length > 20) {
        productName.substring(0, 19)
    } else {
        productName
    }


    private fun processEntregaloResponse(entregaloResponse: AskNewServiceResponse,
                                         customer: Customer, amount: BigDecimal, paymentMethod: String): GenericResponse<*> {

        return if (HttpStatus.OK == entregaloResponse.status || HttpStatus.CREATED == entregaloResponse.status) {
            val baseUrl = apiURL.replace("/api", "")
            val orderConsultUrl = "$baseUrl/tracking/${entregaloResponse.guia}"
            val message = "Tu pedido fue recibido satisfactoriamente. Haz seguimiento de tu pedido ingresando al siguiente enlace: " +
                    "$orderConsultUrl"
            communicationService.sendSmSAndEmail(customer.email,customer.contactNumber,
                    message,orderConsultUrl,TEMPLATE_ORDER_CONFIRMED, ORDER_CONFIRMED, MESSAGE_GUIDE_NUMBER_NOTIFICATION,
                    SUBJECT_ORDER_GENERATION,"Ver estado del pedido")
            GenericResponse.SuccessResponse(EntregaloData(entregaloResponse.guia!!,entregaloResponse.label))
        } else {
            communicationService.sendEmailWithTemplate(receiver = "operativo@mipagoseguro.co", templateName = TEMPLATE_EMAIL_PLANE_TEXT,
                    title = "Error generando guia a orden",
                    o = mapOf(CONST_MESSAGE to
                            "Orden: customerId: ${customer.id}, valor de la compra : ${amount}, paymentMethod: $paymentMethod",
                            CONST_TITLE_BODY to "Es necesario revisar logs y procesar guia manual"))
            GenericResponse.ErrorResponse("Error calling Entregalo")
        }
    }

    fun getCities(): List<CityDTO>? {
        val cities = getCitiesFromDatabase()
        if (cities == null || cities.isEmpty()) {
            val citiesResponse = entregaloClient.getCities()
            return if (citiesResponse != null) {
                GlobalScope.launch {
                    launch { saveCities(citiesResponse) }
                }
                citiesResponse
            } else {
                log.error("Error calling get cities from Entregalo")
                null
            }
        }
        return cities
    }

    private fun saveCities(cityDTOS: List<CityDTO>) {
        val citiesEntity = cityDTOS.map { cityDTO -> cityDTO.toEntity() }
        cityRepository.saveAll(citiesEntity)
    }

    private fun getCitiesFromDatabase(): List<CityDTO> {
        return cityRepository.findAll().map { entity -> entity.toDTO() }
    }

}