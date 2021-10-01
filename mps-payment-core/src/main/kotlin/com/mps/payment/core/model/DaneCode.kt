package com.mps.payment.core.model

import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class DaneCode(
    @Id var id: UUID,
    @Column var departmentCode: String,
    @Column var municipalityCode: String,
    @Column var townCode: String,
    @Column var departmentName: String,
    @Column var municipalityName: String,
    @Column var townName: String,
    @Column var longitude: Double,
    @Column var latitude: Double
) {
    companion object {
        const val ID = "id"
        const val DEPARTMENT_CODE = "departmentCode"
        const val MUNICIPALITY_CODE = "municipalityCode"
        const val TOWN_CODE = "townCode"
    }
}

data class DaneCodeDTO(
    var id: UUID,
    var departmentCode: String,
    var municipalityCode: String,
    var townCode: String,
    var departmentName: String,
    var municipalityName: String,
    var townName: String,
    var longitude: Double,
    var latitude: Double
)

fun DaneCode.toDTO() = DaneCodeDTO(
    id = this.id,
    departmentCode = this.departmentCode,
    municipalityCode = this.municipalityCode,
    townCode = this.townCode,
    departmentName = this.departmentName,
    municipalityName = this.municipalityName,
    townName = this.townName,
    longitude = this.longitude,
    latitude = this.latitude
)