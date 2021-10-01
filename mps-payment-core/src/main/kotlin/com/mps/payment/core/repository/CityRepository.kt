package com.mps.payment.core.repository

import com.mps.payment.core.model.City
import org.springframework.data.repository.CrudRepository
import java.util.*

interface CityRepository:CrudRepository<City, UUID> {
    fun findByDaneCode(code: String) : City
}