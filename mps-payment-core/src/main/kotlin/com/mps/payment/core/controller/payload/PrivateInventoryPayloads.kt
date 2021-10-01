package com.mps.payment.core.controller

import java.util.*
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull


data class CreatePrivateInventoryRequest(
    @get:NotBlank(message = EMAIL_NOT_BLANK) val email: String,
    @get:NotNull(message = PRODUCTID_NOT_NULL) val productId: UUID,
    @get:Min(value = 1, message = MINIMUM_QUANTITY) val quantity: Int
) {
    companion object {
        const val EMAIL_NOT_BLANK = "correo no puede ser vacío"
        const val PRODUCTID_NOT_NULL = "Id de producto no puede ser vacío"
        const val MINIMUM_QUANTITY = "cantidad debe ser mínimo 1"
    }
}

data class UpdatePrivateInventoryRequest(val privateInventoryId: UUID, @get:Min(value = 1, message = MINIMUM_QUANTITY) val quantity: Int) {
    companion object {
        const val MINIMUM_QUANTITY = "cantidad debe ser mínimo 1"
    }
}