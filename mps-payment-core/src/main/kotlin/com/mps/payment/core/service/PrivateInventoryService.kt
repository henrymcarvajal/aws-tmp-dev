package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.ServiceResponse
import com.mps.payment.core.controller.CreatePrivateInventoryRequest
import com.mps.payment.core.controller.UpdatePrivateInventoryRequest
import com.mps.payment.core.model.*
import com.mps.payment.core.repository.PrivateInventoryRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class PrivateInventoryService(
        private val privateInventoryRepository: PrivateInventoryRepository,
        private val merchantService: MerchantService,
        private val productService: ProductService
) {
    companion object {
        const val CREATING_PRIVATE_INVENTORY_MESSAGE_ERROR = "Error inesperado en la creación del inventario privado"
        const val DELETING_PRIVATE_INVENTORY_MESSAGE_ERROR = "Error inesperado en el borrado del inventario privado"
        const val MERCHANT_NON_EXISTENT = "El comercio no existe"
        const val PRODUCT_NON_EXISTENT = "El producto no existe"
        const val PRIVATE_INVENTORY_NON_EXISTENT = "El inventario privado no existe"
        const val PRIVATE_INVENTORY_ALREADY_EXISTS = "El inventario privado ya existe"
        const val INVENTORY_NOT_ENOUGH = "No hay inventario suficiente de producto"
        const val USER_NOT_PRIVATE_INVENTORY_OWNER = "El usuario no puede actualizar el inventario privado porque no es su propietario"
    }

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun createPrivateInventory(createPrivateInventoryRequest: CreatePrivateInventoryRequest, userName: String): ServiceResponse<*> {
        val merchantList = merchantService.findByEmail(userName)
        val merchantIdInSession = merchantList[0].id

        val merchantSearch = merchantService.findByEmail(createPrivateInventoryRequest.email)
        if (merchantSearch.isEmpty()) {
            log.error("createPrivateInventory: merchant does not exist (email=${createPrivateInventoryRequest.email})")
            return ServiceResponse.Error(MERCHANT_NON_EXISTENT)
        }

        val privateInventorySearch = privateInventoryRepository.findByProductIdAndMerchantIdAndSellerMerchantIdAndDisabledFalse(
                createPrivateInventoryRequest.productId,
                merchantIdInSession!!,
                merchantSearch[0].id!!
        )
        if (privateInventorySearch.isPresent) {
            log.error("createPrivateInventory: private inventory already exists (productId=${createPrivateInventoryRequest.productId},merchantId=${merchantIdInSession!!},sellerMerchantId=${merchantSearch[0].id!!})")
            return ServiceResponse.Error(PRIVATE_INVENTORY_ALREADY_EXISTS)
        }

        val productSearchResult = productService.findById(createPrivateInventoryRequest.productId)
        if (productSearchResult.isEmpty) {
            log.error("createPrivateInventory: product not found (productId=${createPrivateInventoryRequest.productId})")
            return ServiceResponse.Error(PRODUCT_NON_EXISTENT)
        }

        val product = productSearchResult.get()
        if (product.inventory < createPrivateInventoryRequest.quantity) {
            log.error("createPrivateInventory: not enough inventory for product (productId=${createPrivateInventoryRequest.productId})")
            return ServiceResponse.Error(INVENTORY_NOT_ENOUGH)
        }

        product.inventory = product.inventory - createPrivateInventoryRequest.quantity

        val privateInventoryDTO = PrivateInventoryDTO(
                merchantId = merchantIdInSession,
                sellerMerchantId = merchantSearch[0].id!!,
                productId = product.id,
                quantity = createPrivateInventoryRequest.quantity
        )
        return try {
            productService.save(product)
            val savedPrivateInventory = privateInventoryRepository.save(privateInventoryDTO.toEntity())
            ServiceResponse.Success(savedPrivateInventory.toDTO())
        } catch (e: Exception) {
            log.error(CREATING_PRIVATE_INVENTORY_MESSAGE_ERROR, e)
            ServiceResponse.Error(CREATING_PRIVATE_INVENTORY_MESSAGE_ERROR)
        }
    }


    fun updatePrivateInventory(updatePrivateInventoryRequest: UpdatePrivateInventoryRequest, userName: String): ServiceResponse<*> {

        val privateInventorySearchResult = privateInventoryRepository.findById(updatePrivateInventoryRequest.privateInventoryId)
        if (privateInventorySearchResult.isEmpty) {
            log.error("updatePrivateInventory: private inventory not found (privateInventoryId=${updatePrivateInventoryRequest.privateInventoryId}")
            return ServiceResponse.Error("Inventario privado no encontrado")
        }

        val merchantList = merchantService.findByEmail(userName)
        val merchantIdInSession = merchantList[0].id

        val privateInventory = privateInventorySearchResult.get()
        if (privateInventory.merchantId != merchantIdInSession) {
            log.error("updatePrivateInventory: user cannot update this inventory, it is not the owner (merchantId=${merchantIdInSession}, inventoryId=${updatePrivateInventoryRequest.privateInventoryId})")
            return ServiceResponse.Error("No eres el propietario de este inventario")
        }

        try {
            val delta = privateInventory.quantity - updatePrivateInventoryRequest.quantity

            val productSearch = productService.findById(privateInventory.productId)
            if (productSearch.isPresent) {
                val product = productSearch.get()
                product.inventory = product.inventory + delta
                if(product.inventory<0){
                    log.error("updatePrivateInventory: There is not enough inventory (productId=${privateInventory.productId}})")
                    return ServiceResponse.Error("No hay inventario suficiente")
                }
                productService.save(product)
            } else {
                log.error("updatePrivateInventory: product not found (productId=${privateInventory.productId}})")
                return ServiceResponse.Error("Producto no encontrado")
            }

            privateInventory.quantity = updatePrivateInventoryRequest.quantity
            privateInventoryRepository.save(privateInventory)
            return ServiceResponse.Success(privateInventory.toDTO())
        } catch (e: Exception) {
            log.error("updateInventory: unexpected error updating inventory", e)
            return ServiceResponse.Error("Error actualizando inventario privado")
        }
    }

    fun deletePrivateInventory(privateInventoryId: UUID, userName: String): ServiceResponse<*> {
        val merchantList = merchantService.findByEmail(userName)
        val merchantIdInSession = merchantList[0].id

        val privateInventorySearchResult = privateInventoryRepository.findById(privateInventoryId)
        if (privateInventorySearchResult.isEmpty) {
            log.error("deletePrivateInventory: private inventory not found (privateInventoryId=${privateInventoryId}")
            return ServiceResponse.Error(PRIVATE_INVENTORY_NON_EXISTENT)
        }

        val privateInventory = privateInventorySearchResult.get()
        if (privateInventory.merchantId != merchantIdInSession) {
            log.error("updatePrivateInventory: user cannot update this inventory, it is not the owner (merchantId=${merchantIdInSession}, inventoryId=${privateInventoryId})")
            return ServiceResponse.Error(USER_NOT_PRIVATE_INVENTORY_OWNER)
        }

        val productSearchResult = productService.findById(privateInventory.productId)
        val product = productSearchResult.get()
        product.inventory = product.inventory + privateInventory.quantity
        privateInventory.quantity = 0

        return try {
            productService.save(product)
            privateInventoryRepository.save(privateInventory)
            ServiceResponse.Success(privateInventory)
        } catch (e: Exception) {
            log.error(DELETING_PRIVATE_INVENTORY_MESSAGE_ERROR, e)
            ServiceResponse.Error(DELETING_PRIVATE_INVENTORY_MESSAGE_ERROR)
        }
    }

    fun findProductsByPrivateInventory(merchantId: UUID): List<*> {
        val privateInventoryList = privateInventoryRepository.findBySellerMerchantIdAndDisabledFalse(merchantId)
        if (privateInventoryList.isEmpty()) {
            return privateInventoryList
        }
        return privateInventoryList.map {
            val optionalProduct = productService.findById(it.productId)
            val product = optionalProduct.get()
            product.inventory = it.quantity
            product
        }
    }

    fun clearPrivateInventoryForProduct(productId: UUID) {
        privateInventoryRepository.findByProductId(productId).forEach {
            it.quantity = 0
            privateInventoryRepository.save(it)
        }
    }

    fun findPrivateInventoryByProvider(providerId: UUID): List<*> {
        val privateInventoryList = privateInventoryRepository.findByMerchantIdAndDisabledFalse(providerId)
        if (privateInventoryList.isEmpty()) {
            return privateInventoryList
        }
        return privateInventoryList.map { privateInventory ->
            val dropSellerName = merchantService.findByIdOrNull(privateInventory.sellerMerchantId)?.name ?: "Vacío"
            val productName = productService.findByIdOrNull(privateInventory.productId)?.name ?: "Vacío"
            PrivateInventoryView(dropSellerName = dropSellerName,
                    id = privateInventory.id,
                    productName = productName,
                    quantity = privateInventory.quantity,
                    creationDate = privateInventory.creationDate
            )
        }
    }

    fun findById(id: UUID) = privateInventoryRepository.findById(id)

    fun findByProductIdAndSellerMerchantIdAndDisabledFalse(
            productId: UUID,
            sellerMerchantId: UUID
    ) = privateInventoryRepository.findByProductIdAndSellerMerchantIdAndDisabledFalse(
            productId,
            sellerMerchantId
    )

    fun save(privateInventory: PrivateInventory) = privateInventoryRepository.save(privateInventory)
}
