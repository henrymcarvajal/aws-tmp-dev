package com.mps.common.dto

import java.math.BigDecimal
import java.util.UUID
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern

const val SPECIAL_CHARACTERS = "#@$!%*?&.,;\\-_"
const val PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[$SPECIAL_CHARACTERS])[A-Za-z\\d$SPECIAL_CHARACTERS]{8,}$"
const val PASSWORD_FORMAT = "La contraseña debe contener mínimo ocho carácteres, al menos una letra mayúscula, une letra minúscula, un número y un carácter especial: $SPECIAL_CHARACTERS"
const val PASSWORD_NOT_NULL = "La contraseña no puede ser nula o vacía"
const val NAME_NOT_NULL = "Nombre no puede ser nulo o vacío"
const val NIT_NOT_NULL = "NIT no puede ser nulo o vacío"
const val EMAIL_NOT_NULL = "Email no puede ser nulo o vacío"
const val CONTACT_REGEX = "(^$|[0-9]{10})"
const val CONTACT_FORMAT = "Número de contacto debe tener diez dígitos"
const val CONTACT_NOT_NULL = "Número de contacto no puede ser nulo o vacío"

data class PutBalanceRequest(
        val merchantId: String,
        val amount: BigDecimal
)

data class MerchantDTO(
        val id: UUID?,
        @get:NotBlank(message = NAME_NOT_NULL) val name: String="",
        @get:NotBlank(message = NIT_NOT_NULL) val nit: String?,
        @get:NotBlank(message = EMAIL_NOT_NULL) val email: String="",
        @get:Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_FORMAT) @get:NotBlank(message = PASSWORD_NOT_NULL) val password: String?,
        @get:Pattern(regexp = CONTACT_REGEX, message = CONTACT_FORMAT) @get:NotBlank(message = CONTACT_NOT_NULL) val contactNumber: String="",
        val fbPixel: String?=null,
        val address:String?,
        val branchCode: Int?,
        val balance: BigDecimal = BigDecimal.ZERO
)