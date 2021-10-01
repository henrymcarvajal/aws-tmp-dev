package com.mps.payment.core.service

import com.mps.payment.core.model.DaneCode
import com.mps.payment.core.model.DaneCodeDTO
import com.mps.payment.core.model.toDTO
import com.mps.payment.core.repository.DaneCodeRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class DaneCodeService(
    private val daneCodeRepository: DaneCodeRepository
) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun findById(id: UUID) = daneCodeRepository.findById(id)

    fun findByTownCode(townCode: String): Optional<DaneCodeDTO> {
        val result = daneCodeRepository.findByTownCode(townCode)
        return when (result.isPresent) {
            true -> Optional.of(result.get().toDTO())
            false -> Optional.empty()
        }
    }

    fun findByMunicipalityCode(municipalityCode: String): List<DaneCodeDTO> {
        val result = daneCodeRepository.findByMunicipalityCode(municipalityCode)
        return transformToDTO(result)
    }

    fun findByDepartmentCode(departmentCode: String): List<DaneCodeDTO> {
        val result = daneCodeRepository.findByDepartmentCode(departmentCode)
        return transformToDTO(result)
    }

    private fun transformToDTO(list: List<DaneCode>): List<DaneCodeDTO> {
        val newList = ArrayList<DaneCodeDTO>()
        list.forEach {
            newList.add(it.toDTO())
        }
        return newList
    }
}