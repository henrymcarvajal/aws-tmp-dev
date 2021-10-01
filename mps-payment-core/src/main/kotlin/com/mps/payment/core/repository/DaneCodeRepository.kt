package com.mps.payment.core.repository

import com.mps.payment.core.model.DaneCode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface DaneCodeRepository : JpaRepository<DaneCode, UUID>, JpaSpecificationExecutor<DaneCode> {
    fun findByTownCode(townCode: String): Optional<DaneCode>
    fun findByMunicipalityCode(municipalityCode: String): List<DaneCode>
    fun findByDepartmentCode(departmentCode: String): List<DaneCode>
}
