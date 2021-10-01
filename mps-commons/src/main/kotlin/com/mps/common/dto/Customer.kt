package com.mps.common.dto

import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern

data class CustomerDTO(
        val id: UUID?, @get:NotBlank(message = "Nombre no puede quedar en blanco o null") val name: String, val lastName: String?,
        @get:NotBlank(message = "email no puede ser null o vacio") val email: String,
        @get:Pattern(regexp="(^$|[0-9]{10})",message = "Número de contacto formato incorrecto") @get:NotBlank(message = "number cant not be empty or null") val contactNumber: String,
        @get:Pattern(regexp="(^$|[0-9]+)",message = "Cédula mal formato") val numberId:String?,
        var address:String?,
        var neighborhood:String?,
        var city:String?,
        var department:String?
)