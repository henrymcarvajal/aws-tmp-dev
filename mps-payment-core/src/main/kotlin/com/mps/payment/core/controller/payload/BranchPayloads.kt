package com.mps.payment.core.controller

import java.util.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern

data class CreateBranchRequest(@get:Valid val branch: BranchData, @get:Valid val contact: ContactData)
data class UpdateBranchRequest(val id: UUID, @get:Valid val branch: BranchData, @get:Valid val contact: ContactData)

data class BranchData(
    @get:NotBlank(message = NAME_NOT_BLANK)
    val name: String,
    @get:NotBlank(message = ADDRESS_NOT_BLANK)
    val address: String,
    @get:NotNull(message = DANECODE_NOT_NULL)
    val daneCodeId: UUID
) {
    companion object {
        const val NAME_NOT_BLANK = "Nombre no puede ser nulo o vacío"
        const val ADDRESS_NOT_BLANK = "Dirección no puede ser nula o vacía"
        const val DANECODE_NOT_NULL = "Código DANE no puede ser nulo o vacío"
    }
}

data class ContactData(
    @get:NotBlank(message = NAME_NOT_BLANK)
    val name: String,
    @get:NotNull(message = IDENTIFICATION_NOT_NULL)
    val identification: Long,
    @get:NotNull(message = CONTACT_NOT_BLANK)
    @get:Pattern(regexp = CONTACT_REGEX, message = CONTACT_FORMAT)
    val phone: String,
    @get:NotBlank(message = EMAIL_NOT_BLANK)
    val email: String
) {
    companion object {
        const val NAME_NOT_BLANK = "Nombre no puede ser nulo o vacío"
        const val IDENTIFICATION_NOT_NULL = "Identificación no puede ser nula o vacía"
        const val CONTACT_REGEX = "(^$|[0-9]{10})"
        const val CONTACT_FORMAT = "Número de contacto debe tener diez dígitos"
        const val CONTACT_NOT_BLANK = "Número de contacto no puede ser nulo o vacío"
        const val EMAIL_NOT_BLANK = "Correo electrónico no puede ser nulo o vacío"
    }
}