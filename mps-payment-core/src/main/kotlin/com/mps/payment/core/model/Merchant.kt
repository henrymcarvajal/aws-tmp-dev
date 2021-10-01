package com.mps.payment.core.model

import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
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

@Entity
data class Merchant(
        @Id var id: UUID?, val name: String, var nit: String?, val email: String, @Column(name = "contact_number") var contactNumber: Long,
         @Column(name = "created_at") var creationDate: LocalDateTime? = LocalDateTime.now(),
        @Column(name = "last_updated") val lastUpdatedDate: LocalDateTime? = LocalDateTime.now(),
        @Column(name = "id_user") var idUser: UUID?,@Column(name = "is_enable") var isEnabled: Boolean?=false,
        @Column(name = "fb_pixel") var fbPixel: String?=null,
        @Column(name = "address") var address: String?=null,
        @Column(name = "branch_code") var branchCode: Int?=null
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
        val branchCode: Int?
)

data class MerchantLandingtDTO(
        @get:NotBlank(message = NAME_NOT_NULL) val name: String,
        @get:NotBlank(message = EMAIL_NOT_NULL) val email: String,
        @get:Pattern(regexp = CONTACT_REGEX, message = CONTACT_FORMAT) @get:NotBlank(message = CONTACT_NOT_NULL) val contactNumber: String
)

fun MerchantLandingtDTO.toMerchantDTO()=MerchantDTO(
        name=this.name, email = this.email,contactNumber = this.contactNumber,password = null, nit = null,id = null,
        address = null, branchCode = null
)

fun Merchant.toDto(): MerchantDTO {
    return MerchantDTO(
            id = this.id, name = this.name, nit = this.nit, email = this.email, password = "", contactNumber = this.contactNumber.toString(),
           fbPixel=this.fbPixel,address=this.address, branchCode= this.branchCode
    )
}

fun Merchant.toPublicDTO() = MerchantLandingtDTO(
        name=this.name, contactNumber = this.contactNumber.toString(), email = this.email
)

fun MerchantDTO.toEntity() = Merchant(
        id = this.id
                ?: UUID.randomUUID(), name = this.name.trim().toLowerCase(), nit = this.nit, email = this.email.trim().toLowerCase(), contactNumber = this.contactNumber.toLong(),
        address = this.address, branchCode = this.branchCode,idUser = null
)