package com.mps.payment.web.controller

import com.mps.common.dto.CustomerDTO
import com.mps.payment.core.service.CustomerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping(path = ["customer"])
class CustomerController(private val customerService: CustomerService) {

    @GetMapping("/order/{id}")
    fun getCustomer(@PathVariable id: UUID): ResponseEntity<CustomerDTO> {
        val customer = customerService.getCustomerByOrderId(id)
        return if (customer == null) {
            ResponseEntity.notFound().build<CustomerDTO>()
        } else {
            ResponseEntity.ok(customer)
        }
    }
}