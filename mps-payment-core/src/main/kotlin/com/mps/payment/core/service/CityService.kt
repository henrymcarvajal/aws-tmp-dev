package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.enum.AccountTypeEnum
import com.mps.payment.core.enum.BankEnum
import com.mps.payment.core.enum.PaymentStateEnum
import com.mps.payment.core.model.*
import com.mps.payment.core.repository.CityRepository
import com.mps.payment.core.repository.MerchantRepository
import com.mps.payment.core.repository.PaymentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Service
class CityService(
        private val cityRepository: CityRepository
) {

    fun findById(id: UUID) = cityRepository.findById(id)

    fun findByDANECode(code: String) = cityRepository.findByDaneCode(code)

    fun findAll() = cityRepository.findAll()
}