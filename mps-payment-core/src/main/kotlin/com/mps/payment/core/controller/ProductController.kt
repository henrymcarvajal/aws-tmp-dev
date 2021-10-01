package com.mps.payment.core.controller

import com.google.gson.Gson
import com.mps.common.dto.GenericResponse
import com.mps.common.dto.PaymentAgree
import com.mps.payment.core.model.ProductDTO
import com.mps.payment.core.model.UpdateInventoryProductRequest
import com.mps.payment.core.model.UpdateProductRequest
import com.mps.payment.core.model.toDTO
import com.mps.payment.core.repository.criteria.SearchCriteria
import com.mps.payment.core.service.PrivateInventoryService
import com.mps.payment.core.service.ProductService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.validation.ConstraintViolation
import javax.validation.Valid
import javax.validation.Validation
import javax.validation.ValidatorFactory
import kotlin.collections.ArrayList


@RestController
@RequestMapping(path = ["product"])
class ProductController(private val productService: ProductService, private val privateInventoryService: PrivateInventoryService) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createProduct(@RequestPart("data") productDtoString: String, @RequestPart("image") file: MultipartFile?): ResponseEntity<*> {

        return try {
            val productDTO =createProductDtoFromString(productDtoString)

            val violations = validateProductDTO(productDTO)

            if (violations.isNotEmpty()) {
                var messages = ArrayList<String>()
                violations.forEach {
                    messages.add(it.message)
                }
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("errorMessages" to messages))
            } else {
                log.info("Create Product: input $productDtoString")
                when (val responseService = productService.createProduct(productDTO, file)) {
                    is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as ProductDTO)
                    is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(mapOf("errorMessage" to responseService.message))
                }
            }
        } catch (e: Exception) {
            log.error("Error creating product", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error creating product")
        }
    }

    @PatchMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateProduct(request: HttpServletRequest, @Valid @RequestPart("data") productDTO: UpdateProductRequest, @RequestPart("image") file: MultipartFile? ): ResponseEntity<*> {
        return try {
            val token = request.getHeader("Authorization").replace("Bearer ","")
            log.info("Update product: $productDTO $file")
            when (val responseService = productService.updateProduct(productDTO,token,file)) {
                is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as ProductDTO)
                is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("errorMessage" to responseService.message))            }

        } catch (e: Exception) {
            log.error("Error updating product", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error updating product")
        }
    }

    @PatchMapping("/inventory")
    fun updateInventoryProduct(request: HttpServletRequest, @Valid @RequestBody updateRequest: UpdateInventoryProductRequest): ResponseEntity<*> {
        return try {
            val token = request.getHeader("Authorization").replace("Bearer ","")
            log.info("Update product: $updateRequest")
            when (val responseService = productService.updateInventoryProduct(updateRequest,token)) {
                is GenericResponse.SuccessResponse -> {
                    val product = responseService.obj as ProductDTO
                    privateInventoryService.clearPrivateInventoryForProduct(productId = product.id!!)
                    ResponseEntity.ok().body(product)
                }
                is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(mapOf("errorMessage" to responseService.message))
            }
        } catch (e: Exception) {
            log.error("Error updating product", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error updating product")
        }
    }

    @PostMapping("/payment")
    fun createPaymentFromProduct(@Valid @RequestBody paymentAgree: PaymentAgree): ResponseEntity<*> {
        return try {
            log.info("createPaymentFromProduct: input $paymentAgree")
            when (val responseService = productService.createPaymentFromProduct(paymentAgree)) {
                is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as String)
                is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(mapOf("errorMessage" to responseService.message))
            }
        } catch (e: Exception) {
            log.error("Error creating payment from product", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error creating payment from product")
        }
    }

    @PostMapping("/delete")
    fun removeProducts(@RequestBody request: DeleteProductsRequest): ResponseEntity<*> {

        return try {
            val ids = request.ids
            log.info("remove products: input $ids")
            if (ids.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("List is empty")
            }
            when (val responseService = productService.removeProductsById(ids)) {
                is GenericResponse.SuccessResponse -> ResponseEntity.ok().body(responseService.obj as String)
                is GenericResponse.ErrorResponse -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(mapOf("errorMessage" to responseService.message))
            }
        } catch (e: Exception) {
            log.error("Error deleting products", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error deleting product")
        }
    }

    @GetMapping("/merchant/{merchantId}")
    fun getProduct(@PathVariable merchantId: UUID): ResponseEntity<*> {
        return try {
            val products = productService.getProductsByMerchant(merchantId)
            if (products.isEmpty()) {
                ResponseEntity.notFound().build()
            } else {
                ResponseEntity.status(HttpStatus.OK)
                        .body(products)
            }
        } catch (e: Exception) {
            log.error("Error getting products", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error getting products")
        }
    }

    @PostMapping("/criteria")
    fun getProductByCriteria(request: HttpServletRequest, @RequestBody productSearchCriteria: List<SearchCriteria>): ResponseEntity<*> {
         return try {
             val token = request.getHeader("Authorization").replace("Bearer ","")
            val products = productService.getProductByCriteria(productSearchCriteria,token)
            if (products.isEmpty()) {
                ResponseEntity.notFound().build()
            } else {
                ResponseEntity.ok(products)
            }
        } catch (e: Exception) {
            log.error("Error getting products", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error getting products")
        }
    }

    @GetMapping("/public/{productId}")
    fun getProductById(@PathVariable productId: String): ResponseEntity<*> {
        return try {
            val product = productService.getProductByShortId(productId)
            if (product.isEmpty()) {
                ResponseEntity.notFound().build()
            } else {
                ResponseEntity.status(HttpStatus.OK)
                        .body(product[0].toDTO())
            }
        } catch (e: Exception) {
            log.error("Error getting product", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error getting product")
        }
    }

    private fun createProductDtoFromString(productDtoString: String): ProductDTO {
        val gson = Gson()
        return gson.fromJson(productDtoString, ProductDTO::class.java)
    }

    private fun validateProductDTO(productDTO: ProductDTO): Set<ConstraintViolation<ProductDTO>> {
        val factory: ValidatorFactory = Validation.buildDefaultValidatorFactory()
        val validator = factory.validator
        return validator.validate(productDTO)
    }
}

data class DeleteProductsRequest(val ids: List<String>)