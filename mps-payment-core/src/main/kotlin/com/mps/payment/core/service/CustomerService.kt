package com.mps.payment.core.service

import com.mps.common.dto.CustomerDTO
import com.mps.payment.core.model.Customer
import com.mps.payment.core.model.toDTO
import com.mps.payment.core.model.toEntity
import com.mps.payment.core.repository.CustomerRepository
import com.mps.payment.core.repository.OrderRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.*

@Service
class CustomerService(private val customerRepository: CustomerRepository,
                      private val orderRepository: OrderRepository) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun createOrReplaceCustomer(customerDTO: CustomerDTO): Customer {
        val customer = customerRepository.findCustomerByContactNumber(customerDTO.contactNumber)
        return if (customer != null) {
            customer.contactNumber = customerDTO.contactNumber.trim()
            customer.email = customerDTO.email.trim().toLowerCase()
            customer.lastName = customerDTO.lastName?.trim()?.toLowerCase()
            customer.name = customerDTO.name.trim().toLowerCase()
            customer.numberId = customerDTO.numberId?.trim()
            customer.address = customerDTO.address?.trim()?.toLowerCase()
            customer.neighborhood = customerDTO.neighborhood?.trim()?.toLowerCase()
            customer.city = customerDTO.city?.trim()?.toLowerCase()
            customer.department = customerDTO.department?.trim()?.toLowerCase()
            customerRepository.save(customer)
        } else {
            customerRepository.save(customerDTO.toEntity())
        }
    }


    @Cacheable(value = ["customers"], key = "#customerId", unless = "#result == null")
    fun getCustomerById(customerId: UUID) = customerRepository.findById(customerId)

    fun getCustomerByOrderId(orderId: UUID):CustomerDTO?{
        val optionalOrder = orderRepository.findById(orderId)
        if(optionalOrder.isEmpty){
            log.error("getCustomerByOrderId:Order does not exist")
            return null
        }
        val optionalCustomer = customerRepository.findById(optionalOrder.get().customerId)
        if(optionalCustomer.isEmpty){
            log.error("getCustomerByOrderId customer does not exist")
            return null
        }
        return optionalCustomer.get().toDTO()
    }

    fun getByContactNumber(contactNumber: String) = customerRepository.getByContactNumber(contactNumber)

}