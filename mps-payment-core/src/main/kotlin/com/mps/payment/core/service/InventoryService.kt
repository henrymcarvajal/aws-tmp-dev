package com.mps.payment.core.service

import com.mps.common.dto.ServiceResponse
import com.mps.payment.core.controller.CreateInventoryRequest
import com.mps.payment.core.controller.UpdateInventoryRequest
import com.mps.payment.core.model.*
import com.mps.payment.core.repository.GeolocalizedInventory
import com.mps.payment.core.repository.InventoryHistoryRepository
import com.mps.payment.core.repository.InventoryRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

@Service
class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val inventoryHistoryRepository: InventoryHistoryRepository,
    private val merchantService: MerchantService,
    private val branchService: BranchService,
    private val productService: ProductService,
    private val daneCodeService: DaneCodeService
) {

    companion object {
        const val FINDING_INVENTORY_MESSAGE_ERROR = "Error inesperado en la búsqueda del inventario"
        const val CREATING_INVENTORY_MESSAGE_ERROR = "Error inesperado en la creación del inventario"
        const val UPDATING_INVENTORY_MESSAGE_ERROR = "Error inesperado en la actualización del inventario"
        const val NO_INVENTORIES_WITH_SUCH_PRODUCT = "No se encontraron inventarios cpon el producto especificado"
        const val TARGET_CITY_NOT_FOUND = "No se encontro la ciudad eespecificada"
    }

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun createInventory(createInventoryRequest: CreateInventoryRequest, userName: String): ServiceResponse<*> {
        return try {
            val inventoryList = mutableListOf<InventoryDTO>()
            createInventoryRequest.inventories.forEach {
                val inventorySearchList = inventoryRepository.findByBranchIdAndProductIdAndDisabledNotNullAndDisabledFalse(
                    it.branchId,
                    createInventoryRequest.productId
                )
                if (inventorySearchList.isNotEmpty()) {
                    log.error("createInventory: inventory already created (branchId=${it.branchId}, productId=${createInventoryRequest.productId})")
                } else {
                    val merchantList = merchantService.findByEmail(userName)
                    val merchantIdInSession = merchantList[0].id

                    val branchSearchResult = branchService.findById(it.branchId)
                    if (branchSearchResult.isPresent) {
                        val branch = branchSearchResult.get()
                        if (branch.merchantId == merchantIdInSession) {

                            val productSearchResult = productService.findById(createInventoryRequest.productId)
                            if (productSearchResult.isPresent) {
                                val product = productSearchResult.get()
                                if (product.merchantId == merchantIdInSession) {
                                    val inventoryDTO = InventoryDTO(
                                        merchantId = merchantIdInSession,
                                        branchId = it.branchId,
                                        productId = createInventoryRequest.productId,
                                        quantity = it.quantity
                                    )
                                    val inventory = inventoryRepository.save(inventoryDTO.toEntity())

                                    val inventoryHistoryDTO = InventoryHistoryDTO(
                                        inventoryId = inventory.id,
                                        quantityBefore = 0,
                                        quantityAfter = it.quantity,
                                        source = "API",
                                        operation = "CREATION"
                                    )
                                    inventoryHistoryRepository.save(inventoryHistoryDTO.toEntity())
                                    inventoryList.add(inventory.toDTO())
                                } else {
                                    log.error("createInventory: user cannot create this inventory, it is not the product' owner (merchantId=${merchantIdInSession}, productId=${createInventoryRequest.productId})")
                                }
                            } else {
                                log.error("createInventory: product not found (productId=${createInventoryRequest.productId})")
                            }
                        } else {
                            log.error("createInventory: user cannot create this inventory, it is not the branch' owner (merchantId=${merchantIdInSession}, branchId=${it.branchId})")
                        }
                    } else {
                        log.error("createInventory: branch not found (branchId=${it.branchId})")
                    }
                }
            }
            ServiceResponse.Success(inventoryList)
        } catch (e: Exception) {
            log.error(CREATING_INVENTORY_MESSAGE_ERROR, e)
            ServiceResponse.Error(CREATING_INVENTORY_MESSAGE_ERROR)
        }
    }

    fun updateInventory(updateInventoryRequests: List<UpdateInventoryRequest>, userName: String): ServiceResponse<*> {
        val merchantList = merchantService.findByEmail(userName)
        val merchantIdInSession = merchantList[0].id

        val result = HashMap<UUID, String>()

        return try {
            updateInventoryRequests.forEach {
                val inventorySearchResult = inventoryRepository
                        .findByBranchIdAndProductIdAndDisabledNotNullAndDisabledFalse(productId = it.productId,
                        branchId = it.branchId)
                if (inventorySearchResult.isEmpty()) {
                    log.error("updateInventory: inventory not found (inventoryId=${merchantIdInSession}")
                    result[it.branchId] = "updateInventory: inventory not found"
                } else {

                    val inventory = inventorySearchResult[0]

                    if (inventory.merchantId != merchantIdInSession) {
                        log.error("updateInventory: user cannot update this inventory, it is not the owner (inventoryId=${inventory.id})")
                        result[inventory.id] = "User cannot update this inventory, it is not the owner"
                    }

                    val inventoryHistoryDTO = InventoryHistoryDTO(
                        inventoryId = inventory.id,
                        quantityBefore = inventory.quantity,
                        quantityAfter = it.quantity,
                        source = "API",
                        operation = "UPDATE"
                    )
                    inventory.quantity = it.quantity

                    try {
                        inventoryRepository.save(inventory)
                        inventoryHistoryRepository.save(inventoryHistoryDTO.toEntity())
                        result[inventory.id] = "OK"
                    } catch (e: Exception) {
                        log.error(UPDATING_INVENTORY_MESSAGE_ERROR, e)
                        result[inventory.id] = UPDATING_INVENTORY_MESSAGE_ERROR
                    }
                }
            }
            ServiceResponse.Success(result)
        } catch (e: Exception) {
            log.error(UPDATING_INVENTORY_MESSAGE_ERROR, e)
            ServiceResponse.Error(UPDATING_INVENTORY_MESSAGE_ERROR)
        }
    }

    fun findByProductId(productId: UUID): ServiceResponse<*> {
        return try {
            val result = inventoryRepository.findByProductIdAndDisabledNotNullAndDisabledFalse(productId)
            ServiceResponse.Success(result)
        } catch (e: Exception) {
            log.error(FINDING_INVENTORY_MESSAGE_ERROR, e)
            ServiceResponse.Error(FINDING_INVENTORY_MESSAGE_ERROR)
        }
    }

    fun findNearestByProductId(daneCodeId: UUID, productId: UUID, quantity: Int): ServiceResponse<*> {
        try {
            val inventoryList = inventoryRepository.findByProductIdGeolocalized(productId)

            if (inventoryList.isEmpty()) {
                log.error("findNearestByProductId: no inventories with product (productId=${productId})")
                return ServiceResponse.Error(NO_INVENTORIES_WITH_SUCH_PRODUCT)
            }

            val citySearchResult = daneCodeService.findById(daneCodeId)
            if (citySearchResult.isEmpty) {
                log.error("findNearestByProductId: target city not found (daneCodeId=${daneCodeId})")
                return ServiceResponse.Error(TARGET_CITY_NOT_FOUND)
            }
            val targetCity = citySearchResult.get()

            val nearestGeolocalizedInventory = getNearestCityByDistance(targetCity, inventoryList, quantity)

            return ServiceResponse.Success(nearestGeolocalizedInventory)
        } catch (e: Exception) {
            log.error(FINDING_INVENTORY_MESSAGE_ERROR, e)
            return ServiceResponse.Error(FINDING_INVENTORY_MESSAGE_ERROR)
        }
    }

    private fun getNearestCityByDistance(targetCity: DaneCode, originCities: List<GeolocalizedInventory>, quantity: Int): GeolocalizedInventory {

        var currentDistance = Double.MAX_VALUE
        var nearestInventory = originCities[0]

        originCities.forEach {
            val distance = getDistance(targetCity.latitude, it.latitude!!, targetCity.longitude, it.longitude!!)
            if (distance < currentDistance) {
                currentDistance = distance
                if (quantity <= it.quantity!!) {
                    nearestInventory = it
                }
            }
        }

        return nearestInventory
    }

    private fun getDistance(latitude1: Double, latitude2: Double, longitude1: Double, longitude2: Double): Double {
        return sqrt(((latitude1 - latitude2).pow(2.0) + (longitude1 - longitude2).pow(2.0)))
    }
}
