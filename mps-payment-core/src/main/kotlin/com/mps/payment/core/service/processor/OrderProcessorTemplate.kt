package com.mps.payment.core.service.processor

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.OrderDropDTO
import com.mps.payment.core.enum.OrderStatus
import com.mps.payment.core.enum.PaymentMethod
import com.mps.payment.core.model.*
import com.mps.payment.core.repository.OrderRepository
import com.mps.payment.core.service.CustomerService
import com.mps.payment.core.service.DropshippingSaleService
import com.mps.payment.core.service.InventoryProcessorService
import com.mps.payment.core.service.PriceService
import org.springframework.beans.factory.annotation.Autowired

abstract class OrderProcessorTemplate {
    @Autowired
    private lateinit var customerService: CustomerService

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    protected lateinit var inventoryProcessorService: InventoryProcessorService

    @Autowired
    private lateinit var dropshippingSaleService: DropshippingSaleService

    @Autowired
    private lateinit var priceService: PriceService

    fun setDependencies(customerService: CustomerService,orderRepository: OrderRepository,
                inventoryProcessorService: InventoryProcessorService,dropshippingSaleService: DropshippingSaleService,
                 priceService: PriceService){
        this.customerService = customerService
        this.orderRepository = orderRepository
        this.inventoryProcessorService = inventoryProcessorService
        this.dropshippingSaleService = dropshippingSaleService
        this.priceService = priceService
    }


    fun preProcessOrder(orderDropDTO: OrderDropDTO): GenericResponse<*> {
        val dropSale = dropshippingSaleService.getDropshippingSaleById(orderDropDTO.productId)
        if (dropSale.isEmpty()) {
            return GenericResponse.ErrorResponse("No existe el producto")
        }
        val customer = customerService.createOrReplaceCustomer(orderDropDTO.customer!!)
        val orderEntity = orderDropDTO.toEntity(dropShippingSale = dropSale[0], customerId = customer.id!!)
        orderDropDTO.id = orderEntity.id
        orderEntity.customerId = customer.id!!
        orderEntity.dropShippingSale = dropSale[0]
        var merchantSeller = dropSale[0].merchant
        val product = dropSale[0].product
        val basePrice = dropSale[0].amount ?: product.amount
        orderDropDTO.amount = priceService.getOrderPrice(dropSale[0].id, orderDropDTO.quantity, basePrice, dropSale[0].specialConditions)
        orderEntity.amount = orderDropDTO.amount
        val pairResult = inventoryProcessorService.processPrivateInventoryForOrder(customer.city!!,
                orderDropDTO.quantity, product.id, dropSale[0].merchant.id!!, product.inventory)
        val branchCode = pairResult.first ?: return processPreOrderError(orderEntity, "There is not inventory")
        orderDropDTO.branchCode = branchCode
        orderEntity.branchCode = branchCode
        return GenericResponse.SuccessResponse(PreProcessOrderResponse(orderDTO = orderDropDTO, orderEntity = orderEntity,
                product = product, customer = customer, sellerMerchant = merchantSeller, privateInventory = pairResult.second))
    }

    abstract fun process(orderDropDTO: OrderDropDTO? = null, product: Product? = null,
                         generalOrderDropEntity: GeneralOrderDrop? = null,
                         customer: Customer? = null, sellerMerchant: Merchant? = null)

    fun postOrder(order: GeneralOrderDrop, product: Product, privateInventory: PrivateInventory?): GenericResponse<*> {
        if (OrderStatus.FAILED.state == order.orderStatus) {
            return GenericResponse.ErrorResponse("Error processing order")
        }
        if (PaymentMethod.COD.method == order.paymentMethod) {
            return GenericResponse.SuccessResponse(order.toDTO())
        }
        if (PaymentMethod.ONLINE.method == order.paymentMethod) {
            inventoryProcessorService.decreaseInventory(product, order, privateInventory)
            return GenericResponse.SuccessResponse(order.toDTO())
        }
        return if (order.paymentMethod == PaymentMethod.MPS.method && order.paymentId.isNullOrBlank()) {
            GenericResponse.ErrorResponse("Error generating payment from product")
        } else {
            inventoryProcessorService.decreaseInventory(product, order, privateInventory)
            GenericResponse.SuccessResponse(order.paymentId)
        }
    }

    private fun processPreOrderError(generalOrderDropEntity: GeneralOrderDrop, message: String): GenericResponse.ErrorResponse {
        generalOrderDropEntity.orderStatus = OrderStatus.FAILED.state
        orderRepository.save(generalOrderDropEntity)
        return GenericResponse.ErrorResponse(message)
    }

}

data class PreProcessOrderResponse(
        val orderDTO: OrderDropDTO,
        val orderEntity: GeneralOrderDrop,
        val product: Product,
        val sellerMerchant: Merchant,
        val customer: Customer,
        val privateInventory: PrivateInventory?
)