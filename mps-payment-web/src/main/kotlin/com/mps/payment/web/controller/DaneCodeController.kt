package com.mps.payment.web.controller

import com.mps.payment.core.service.DaneCodeService
import com.mps.payment.core.util.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping(path = ["dane"])
class DaneCodeController(
    private val daneCodeService: DaneCodeService
) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/town/{townCode}")
    fun getTown(@PathVariable townCode: String): ResponseEntity<*> {
        return try {
            val serviceResponse = daneCodeService.findByTownCode(townCode)
            when (serviceResponse.isPresent) {
                true -> ResponseEntity.ok().body(serviceResponse.get())
                false -> ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            log.error("Error getting town", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error getting town: ${ExceptionUtils.toString(e)}")
        }
    }

    @GetMapping("/municipality/{municipalityCode}")
    fun getMunicipality(@PathVariable municipalityCode: String): ResponseEntity<*> {
        return try {
            val serviceResponse = daneCodeService.findByMunicipalityCode(municipalityCode)
            ResponseEntity.ok().body(serviceResponse)
        } catch (e: Exception) {
            log.error("Error getting municipality", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error getting municipality: ${ExceptionUtils.toString(e)}")
        }
    }

    @GetMapping("/department/{departmentCode}")
    fun getDepartment(@PathVariable departmentCode: String): ResponseEntity<*> {
        return try {
            val serviceResponse = daneCodeService.findByDepartmentCode(departmentCode)
            ResponseEntity.ok().body(serviceResponse)
        } catch (e: Exception) {
            log.error("Error getting department", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error getting department: ${ExceptionUtils.toString(e)}")
        }
    }
}