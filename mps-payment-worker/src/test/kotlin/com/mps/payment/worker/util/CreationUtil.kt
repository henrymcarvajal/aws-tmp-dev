package com.mps.payment.worker.util

import com.mps.common.dto.*
import com.mps.payment.core.client.entregalo.payload.*
import com.mps.payment.core.model.*
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

fun createPaymentTest(merchantId: UUID = UUID.randomUUID(), id: UUID = UUID.randomUUID(),
                      amount : BigDecimal = BigDecimal.valueOf(100000), idState:Int=1) = PaymentDTO(
    amount = amount, id = id, idState = idState, idCustomer = UUID.randomUUID(),
    idMerchant = merchantId, linkUrl = "https://payco.link/123455",
    guideNumber = "1224455", transportCompany = "servidelivery",description = "desc"
)

fun createPaymentAgreeTest(paymentId: String = UUID.randomUUID().toString()) = PaymentAgree(
    idPayment = paymentId, customer = createCustomerTest()
)

fun createCustomerTest(id: UUID = UUID.randomUUID(), numberId: String = "") = CustomerDTO(
    contactNumber = "3014887102",
    email = "test@test.com",
    id = id,
    lastName = "Esp",
    name = "JL",
    numberId = numberId,
    address = "address",
    neighborhood = "",
    city = "",
    department = ""
)

fun createMerchantTest(id: UUID? = UUID.randomUUID(), nit: String = "123456") = MerchantDTO(
    id = id, nit = nit, name = "name test", contactNumber = "123", email = "jorjek4@hotmail.com",
    password = "Abc@123", address = "address",
    branchCode = 1234
)

fun createUpdateBankingInformationRequest(merchantId: UUID, id: UUID?=null)=CreateBankingInformationRequest(
    merchantId = merchantId, accountNumber = 12345, accountType = 1, accountBank = 2,
    documentType = 1, documentNumber = 1234554, fullName = "Leon",id = id
)

fun createBankingInformation(merchantId: UUID=UUID.randomUUID(), id: UUID?=null)=BankingInformation(
    merchantId = merchantId, accountNumber = 12345, accountType = 1, accountBank = 2,
    documentType = 1, documentNumber = 1234554, fullName = "Leon",id = id?:UUID.randomUUID()
)

fun createWithdrawalTest(merchantId: UUID = UUID.randomUUID(), amount: BigDecimal = BigDecimal.valueOf(100000)) = WithdrawalDTO(
    id=null, idMerchant = merchantId, amount = amount
)

fun <T> anyObject(): T {
    return Mockito.anyObject<T>()
}

fun createOrderDTOTest(quantity:Int=1,paymentMethod:String="COD",guideNumber:Int?=null,orderStatus: Int=1) = OrderDropDTO(
    amount = BigDecimal.valueOf(85000),
    id=null,
    creationDate = LocalDateTime.now(),
    modificationDate = LocalDateTime.now(),
    orderStatus = orderStatus,
    guideNumber = guideNumber,
    comision = BigDecimal.ZERO,
    paymentMethod = paymentMethod,
    productId = UUID.randomUUID(),
    customer = null,
    quantity = quantity,
    paymentId = null,
    freightPrice = null,
    observations = "observations",
    branchCode = 100,
    isLabeled = false,
    sellerName = "mipagoseguro",
    label = "url label"
)

fun createInternalNumberTest()= InternalNumbers(
    id=UUID.randomUUID(),
    freightCommission = BigDecimal(10000),
    paymentMethod = "COD",
    paymentCommission = BigDecimal.ZERO,
    creationDate = LocalDateTime.now(),
    modificationDate = LocalDateTime.now(),
    orderId = UUID.randomUUID()
)

fun createCustomerTest(id:UUID= UUID.randomUUID()) = CustomerDTO(id=id,name="jorge",lastName = "esp",email = "ams@ams.com",contactNumber = "123456",
    numberId = "123456789",address = "calle",neighborhood = "alt",city = "bogot",department = "cund")

fun createProductTest(inventory:Int=0, id: UUID = UUID.randomUUID()) = Product(
    amount = BigDecimal(100000),
    description = "test product",
    id=id,
    merchantId = UUID.randomUUID(),
    creationDate = LocalDateTime.now(),
    inventory = inventory,
    imageUrl = "",
    dropshipping = true,
    disabled = false,
    deletionDate = null,
    name="product name",
    dropshippingPrice = BigDecimal.ZERO,
    specialFeatures = false,
    category = 1
)

fun createAskNewServiceResponseSuccessful() = AskNewServiceResponse(
    status = HttpStatus.OK,
    guia = "1234",
    label = "label"
)

fun createAskNewServiceResponseFail() = AskNewServiceResponse(
    status = HttpStatus.INTERNAL_SERVER_ERROR
)

object MockitoHelper {
    fun <T> anyObject(): T {
        Mockito.any<T>()
        return uninitialized()
    }
    @Suppress("UNCHECKED_CAST")
    fun <T> uninitialized(): T =  null as T
}

fun createGetCitiesResponse() = GetCitiesResponse(error = false,
    messages = arrayOf(), data = createCities()
)

fun createCity() = CityDTO(state = "Huila", city = "Neiva",
    code = "11001000", cityExtended = null, againstDelivery = "ACTIVE")

fun createCities() = Cities(listOf())

fun createQueryServiceStatusResponse(status:HttpStatus,serviceStatus:String="Process") = QueryServiceStatusResponse(
    status = status,freightPrice = BigDecimal(10000),serviceStatus = serviceStatus

)

fun createProductDropshippingSale(checkoutId:UUID)=
    ProductDropSale(
        buyPrice = BigDecimal(10000),
        id = checkoutId,
        sellPrice = BigDecimal(12000),
        name = "product test",
        inventory = 3,
        productId = UUID.randomUUID()
    )

fun createDropSateTest(productId:UUID=UUID.randomUUID(), id:UUID=UUID.randomUUID(), disabled:Boolean=false) = DropshippingSale(
    id = id,
    product = createProductTest(id=productId),
    amount = BigDecimal(50000),
    merchant = createMerchantTest().toEntity(),
    specialConditions = "null",
    creationDate = LocalDateTime.now(),
    disabled = disabled,
    deletionDate = LocalDateTime.now()
)

fun createGeneralOrderTest(customerId:UUID,productId:UUID=UUID.randomUUID(), orderStatus:Int=3,dropSaleId:UUID=UUID.randomUUID()) = GeneralOrderDrop(
    amount= BigDecimal(50000),
    customerId = customerId,
    id=UUID.randomUUID(),
    orderStatus = orderStatus,
    creationDate = LocalDateTime.now(),
    modificationDate = LocalDateTime.now(),
    paymentMethod = "COD",
    comision = BigDecimal(500),
    freightPrice = BigDecimal(10000),
    quantity = 2,
    dropShippingSale = createDropSateTest(productId=productId,id = dropSaleId),
    branchCode = 100,
    isLabeled = false,
    label=""
)

fun createUpdateOrderRequest(orderId:UUID=UUID.randomUUID()) = UpdateOrderRequest(
    id=orderId,customer = createCustomerTest()
)