package com.mps.payment.core.service

import com.mps.common.dto.ServiceResponse
import com.mps.payment.core.model.DaneCodeDTO
import com.mps.payment.core.model.GeneralOrderDrop
import com.mps.payment.core.model.PrivateInventory
import com.mps.payment.core.model.Product
import com.mps.payment.core.repository.GeolocalizedInventory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class InventoryProcessorService(
        private val privateInventoryService: PrivateInventoryService,
        private val daneCodeService: DaneCodeService,
        private val inventoryService: InventoryService,
        private val productService: ProductService
) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun processPrivateInventoryForOrder(city: String, quantity: Int, productId: UUID, sellerMerchantId: UUID, productInventory: Int): Pair<Int?, PrivateInventory?> {
        val citySearch = daneCodeService.findByTownCode(city)
        val privateInventorySearch = privateInventoryService.findByProductIdAndSellerMerchantIdAndDisabledFalse(
                productId,
                sellerMerchantId
        )
        if (privateInventorySearch.isPresent && quantity <= privateInventorySearch.get().quantity) {
            val privateInventory = privateInventorySearch.get()
            if (quantity > privateInventory.quantity) {
                log.error("No hay inventario suficiente en el inventario privado")
                return Pair(null, null)
            }
            /* --- lookup general inventory --- */
            return Pair(verifyInventoryWithBranch(citySearch.get(), productId, quantity), privateInventory)
            /* --- lookup general inventory --- */
        } else {
            if (quantity > productInventory) {
                log.error("Error detecting nearest branch")
                return Pair(null, null)
            }
            return Pair(verifyInventoryWithBranch(citySearch.get(), productId, quantity), null)
        }

    }

    private fun verifyInventoryWithBranch(citySearch: DaneCodeDTO, productId: UUID, quantity: Int): Int? =
            when (val serviceResponse = inventoryService.findNearestByProductId(citySearch.id, productId, quantity)) {
                is ServiceResponse.Success -> {
                    val geolocalizedInventory = serviceResponse.obj as GeolocalizedInventory
                    if (0 == geolocalizedInventory.quantity) {
                        log.error("No hay inventario en la sucursal calculada")
                        null
                    }
                    geolocalizedInventory.branchCode ?: 0
                }
                is ServiceResponse.Error -> {
                    log.error("Error detecting nearest branch")
                    null
                }
            }

    fun decreaseInventory(product: Product, order: GeneralOrderDrop, privateInventory: PrivateInventory?) {
        privateInventory?.let {
            it.quantity = it.quantity - order.quantity
            privateInventoryService.save(it)
        } ?: let {
            product.inventory = product.inventory - order.quantity
            productService.save(product)
        }

    }

    fun decreaseInventoryCOD(product: Product, order: GeneralOrderDrop){
        val privateInventory = validatePrivateInventory(product.id,order.dropShippingSale.merchant.id!!,order.quantity)
        decreaseInventory(product,order,privateInventory)
    }

    private fun validatePrivateInventory(productId: UUID, sellerMerchantId: UUID, quantity: Int): PrivateInventory? {
        val privateInventorySearch = privateInventoryService.findByProductIdAndSellerMerchantIdAndDisabledFalse(
                productId,
                sellerMerchantId
        )
        return if (privateInventorySearch.isPresent && quantity <= privateInventorySearch.get().quantity) {
            privateInventorySearch.get()
        } else {
            null
        }
    }
}