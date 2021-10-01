package com.mps.payment.core.repository

import com.mps.payment.core.model.Customer
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CustomerRepository: CrudRepository<Customer, UUID> {

    @Query(value = "SELECT * FROM Customer c WHERE c.number_id = :number_id", nativeQuery = true)
    fun findCustomerByNumberId(
            @Param("number_id") numberId: String): Customer?

    @Query(value = "SELECT * FROM Customer c WHERE c.contact_number = :contact_number", nativeQuery = true)
    fun findCustomerByContactNumber(
            @Param("contact_number") contact_number: String): Customer?

    fun getByContactNumber(contactNumber:String):Optional<Customer>
}