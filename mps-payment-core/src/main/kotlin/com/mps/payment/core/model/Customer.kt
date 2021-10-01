package com.mps.payment.core.model

import com.mps.common.dto.CustomerDTO
import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id


@Entity
data class Customer(
        @Id val id: UUID?, var name: String, var lastName: String?, var email: String, @Column(name = "contact_number") var contactNumber: String,
        @Column(name = "created_at") val creationDate: LocalDateTime?= LocalDateTime.now(),
        @Column(name = "last_updated") val lastUpdatedDate: LocalDateTime,
        @Column(name="number_id") var numberId:String?,
        @Column(name="address") var address:String?,
        @Column(name="neighborhood") var neighborhood:String?,
        @Column(name="city") var city:String?,
        @Column(name="department") var department:String?
)



fun Customer.toDTO() = CustomerDTO(
        id = this.id, name = this.name, lastName = this.lastName, email = this.email, contactNumber = this.contactNumber,
        numberId = this.numberId, address= this.address, neighborhood=this.neighborhood, city=this.city,
        department = this.department
)

fun CustomerDTO.toEntity() = Customer(
        id = this.id?: UUID.randomUUID(), name = this.name.trim().toLowerCase(), lastName = this.lastName?.trim()?.toLowerCase(), email = this.email.trim().toLowerCase(), contactNumber = this.contactNumber,
        lastUpdatedDate = LocalDateTime.now(), numberId = this.numberId,address = this.address,
        neighborhood = this.neighborhood, city = this.city, department = this.department
)
