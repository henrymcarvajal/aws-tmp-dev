package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.model.*
import com.mps.payment.core.repository.DropshippingSaleRepository
import com.mps.payment.core.repository.OrderRepository
import com.mps.payment.core.security.jwt.JwtTokenProvider
import net.bytebuddy.description.type.TypeList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


@Service
class DropshippingSaleService(
    private val dropshippingSaleRepository: DropshippingSaleRepository,
    private val productService: ProductService,
    private val merchantService: MerchantService,
    private val privateInventoryService: PrivateInventoryService,
    private val discountRuleParser: DiscountRuleParser,
    private val jwtTokenProvider: JwtTokenProvider,
    private val orderRepository: OrderRepository
) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun findByProductIdInAndDisabledFalse(productIds: List<UUID>) = dropshippingSaleRepository.findByProductIdInAndDisabledFalse(productIds)

    fun createDropshippingSale(dropshippingSaleDTO: DropshippingSaleDTO, token: String): GenericResponse<*> {
        val userName = jwtTokenProvider.getUsername(token)
        val merchantList = merchantService.findByEmail(userName)
        if (merchantList.isEmpty()) {
            log.error("createDropshippingSale: merchant does not exist")
            return GenericResponse.ErrorResponse("Comercio no existe")
        }
        dropshippingSaleDTO.sellerMerchantId = merchantList[0].id
        val product = productService.findById(dropshippingSaleDTO.productId)
        if (product.isEmpty) {
            log.error("createDropshippingSale: product does not exist")
            return GenericResponse.ErrorResponse("Producto no existe")
        }
        if (product.get().merchantId == dropshippingSaleDTO.sellerMerchantId) {
            log.error("createDropshippingSale: you can not do dropshipping of your own product")
            return GenericResponse.ErrorResponse("you can not do dropshipping of your own product")
        }

        var dropshippingSalesList = dropshippingSaleRepository.findByProductIdAndSellerMerchantId(dropshippingSaleDTO.productId, dropshippingSaleDTO.sellerMerchantId!!)
        if (dropshippingSalesList.isEmpty()) {
            val dropshippingSale = dropshippingSaleRepository.save(dropshippingSaleDTO.toEntity(product.get(),merchantList[0]))
            return GenericResponse.SuccessResponse(dropshippingSale.toDTO())
        } else {
            log.error("createDropshippingSale: this dropshipping sale already exists")
            return GenericResponse.ErrorResponse("this dropshipping sale already exists")
        }
    }

    fun updateDropshippingSale(dropshippingSaleDTO: DropshippingSaleDTO): GenericResponse<*> {
        if (dropshippingSaleDTO.id == null) {
            log.error("updateDropshippingSale: merchant does not exist")
            return GenericResponse.ErrorResponse("id es nulo")
        }

        val merchant = merchantService.getMerchant(dropshippingSaleDTO.sellerMerchantId!!)
        val product = productService.findById(dropshippingSaleDTO.productId)

        if (merchant.isEmpty) {
            log.error("updateDropshippingSale: merchant does not exist")
            return GenericResponse.ErrorResponse("Comercio no existe")
        }

        if (product.isEmpty) {
            log.error("updateDropshippingSale: product does not exist")
            return GenericResponse.ErrorResponse("Producto no existe")
        }

        val dropshippingSale = dropshippingSaleRepository.save(dropshippingSaleDTO.toEntity(product.get(),merchant.get()))

        return GenericResponse.SuccessResponse(dropshippingSale.toDTO())
    }

    fun updateAmountDropshippingSale(updateAmountRequest: UpdateAmountDropSale): GenericResponse<*> {

        val dropSale = dropshippingSaleRepository.findById(updateAmountRequest.id)
        if (dropSale.isEmpty) {
            return GenericResponse.ErrorResponse("Checkout no existe")
        }
        val dropSaleVal = dropSale.get()
        dropSaleVal.amount = updateAmountRequest.amount
        return GenericResponse.SuccessResponse(dropshippingSaleRepository.save(dropSaleVal).toDTO())
    }

    fun removeDropshippingSalesById(ids: List<String>): GenericResponse<*> {
        val dropshippingSales = arrayListOf<DropshippingSale>()
        ids.forEach {
            val dropshippingSaleSearchResult = dropshippingSaleRepository.findById(UUID.fromString(it))
            if (dropshippingSaleSearchResult.isEmpty) {
                log.error("error deleting dropshippingSales, dropshippingSale not found: [id: $it]")
            } else {
                val dropshippingSale = dropshippingSaleSearchResult.get()
                dropshippingSale.disabled = true
                dropshippingSale.deletionDate = LocalDateTime.now()
                dropshippingSales.add(dropshippingSale)
            }
        }
        dropshippingSaleRepository.saveAll(dropshippingSales)
        return GenericResponse.SuccessResponse("DropshippingSales deleted successfully")
    }

    fun getDropshippingSaleById(dropshippingSaleId: UUID) = dropshippingSaleRepository.findByIdEqualsAndDisabledFalse(dropshippingSaleId)

    fun getDropshippingSaleBySellerMerchantId(sellerMerchantId: UUID, returnDisabled: Boolean = false): List<ProductDropSale> {
        val list = if (returnDisabled) {
            dropshippingSaleRepository.findBySellerMerchantId(sellerMerchantId)
        } else {
            dropshippingSaleRepository.findBySellerMerchantIdAndDisabledFalse(sellerMerchantId)
        }

        return list.map {
            val productRecord = it.product
            val privateInventory = privateInventoryService.findByProductIdAndSellerMerchantIdAndDisabledFalse(productRecord.id, sellerMerchantId)
            if (privateInventory.isPresent) {
                productRecord.inventory = privateInventory.get().quantity
            }
            it.toProductSale(productRecord.toDTO())
        }

    }

    fun getResultForDropshipper(merchantId: UUID, initialDate: LocalDate, finalDate: LocalDate): GenericResponse<*> {
        if (initialDate.isAfter(finalDate)) {
            return GenericResponse.ErrorResponse("Fecha inicial debe ser antes de fecha final")
        }

        val dropsales = dropshippingSaleRepository.findBySellerMerchantIdAndDisabledFalse(merchantId)
        val productIds = dropsales.map { it.id }

        val orders = if (initialDate.isEqual(finalDate)) {
            orderRepository.getConsolidateBySingleDateGroupByStatus(productIds, initialDate)
        } else {
            orderRepository.getConsolidateByIntervalDateGroupByStatus(productIds, initialDate, finalDate)
        }

        return GenericResponse.SuccessResponse(orders.map {
            OrderConsolidateGroupByStatus(
                    status = it.getStatus(),
                    totalAmount = it.getTotalAmount(),
                    profit = it.getTotalProfit(),
                    quantity = it.getQuantity()
            )
        })
    }

    fun getDropshippingSaleCheckoutViewByShortId(dropshippingSaleId: UUID): DropshippingSaleCheckoutView? {

        val dropshippingSaleSearchResult = dropshippingSaleRepository.findById(dropshippingSaleId)

        return if (!dropshippingSaleSearchResult.isEmpty) {

            val dropshippingSale = dropshippingSaleSearchResult.get()
            val merchant = dropshippingSale.merchant
            val product = dropshippingSale.product
            val discountRules = dropshippingSale.specialConditions?.let { discountRuleParser.parseDiscountRules(it) }

            val discounts = mutableListOf<DiscountView>()

            if (discountRules != null) {
                for (discountRule: DiscountRule in discountRules) {
                    val discountView = DiscountView(discountRule.quantity, discountRule.discount.amount)
                    discounts.add(discountView)
                }
            }

            DropshippingSaleCheckoutView(
                    product.name ?: "Sin nombre",
                    product.description,
                    merchant.name,
                    product.imageUrl,
                    dropshippingSale.amount ?: product.amount,
                    specialConditions = product.specialFeatures,
                    discounts = discounts,
                    fbId = merchant.fbPixel ?: ""
            )
        } else {
            null
        }
    }

    fun findById(id: UUID) = dropshippingSaleRepository.findById(id)
}

data class DiscountView(
    val quantity: Int,
    val finalPrice: Int
)