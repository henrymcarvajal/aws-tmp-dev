package com.mps.payment.core.controller

import java.util.*
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

data class CreateInventoryRequest(@get:NotNull(message = PRODUCTID_NOT_NULL) val productId: UUID, @get:Valid val inventories: List<@Valid InventoryItem>) {
    companion object {
        const val PRODUCTID_NOT_NULL = "Id de producto no puede ser vacío"
    }
}

data class InventoryItem(@get:NotNull(message = BRANCHID_NOT_NULL) val branchId: UUID, @get:Min(value = 1, message = MINIMUM_QUANTITY) val quantity: Int) {
    companion object {
        const val BRANCHID_NOT_NULL = "Id de sucursal no puede ser vacío"
        const val MINIMUM_QUANTITY = "cantidad debe ser mínimo 1"
    }
}

data class UpdateInventoryRequest(@get:NotNull(message = PRODUCTID_NOT_NULL) val productId: UUID, @get:NotNull(message = BRANCHID_NOT_NULL) val branchId: UUID, @get:Min(value = 1, message = MINIMUM_QUANTITY) val quantity: Int) {
    companion object {
        const val PRODUCTID_NOT_NULL = "Id de producto no puede ser vacío"
        const val BRANCHID_NOT_NULL = "Id de branch no puede ser vacío"
        const val MINIMUM_QUANTITY = "cantidad debe ser mínimo 1"
    }
}

data class GetNearestInventoryRequest(val daneCodeId: UUID, val productId: UUID, val quantity: Int)