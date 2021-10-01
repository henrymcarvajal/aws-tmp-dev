package com.mps.payment.core.repository

import com.mps.payment.core.model.InternalNumbers
import org.springframework.data.repository.CrudRepository
import java.util.*

interface InternalNumberRepository : CrudRepository<InternalNumbers, UUID> {
    fun findByOrderId(orderId:UUID):List<InternalNumbers>
}