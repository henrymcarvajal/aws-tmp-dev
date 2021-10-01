package com.mps.payment.core.service.fundsdispersion.file

import org.apache.poi.ss.usermodel.Workbook

data class GeneratedDispersionFiles (
    val summary: Workbook,
    val details: Map<List<String>, Workbook>
)
