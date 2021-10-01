package com.mps.payment.core.service

import com.mps.common.dto.GenericResponse
import com.mps.common.dto.PaymentAgree
import com.mps.common.dto.PaymentDTO
import com.mps.common.util.img.compressImage
import com.mps.common.util.img.convertMultiPartToFile
import com.mps.payment.core.client.s3.AmazonClient
import com.mps.payment.core.email.EmailSender
import com.mps.payment.core.model.*
import com.mps.payment.core.repository.DropshippingSaleRepository
import com.mps.payment.core.repository.MerchantRepository
import com.mps.payment.core.repository.ProductRepository
import com.mps.payment.core.repository.criteria.GenericSpecification
import com.mps.payment.core.repository.criteria.SearchCriteria
import com.mps.payment.core.security.jwt.JwtTokenProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.FileInputStream
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.imageio.ImageIO


const val SUBJECT_PRODUCT_REMOVED = "El proveedor ha eliminado un producto"
const val TEMPLATE_EMAIL_PLANE_TEXT = "plane_template_text"
const val PRODUCT_WIDTH = 800
const val PRODUCT_WIDTH_MIN = 230

@Service
class ProductService(private val productRepository: ProductRepository,
                     private val merchantRepository: MerchantRepository,
                     private val paymentService: PaymentService,
                     private val amazonClient: AmazonClient,
                     private val dropshippingSaleRepository: DropshippingSaleRepository,
                     private val emailSender: EmailSender,
                     private val tokenProvider: JwtTokenProvider) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun createProduct(productDTO: ProductDTO, multipartFile: MultipartFile?): GenericResponse<*> {
        if (!merchantRepository.existsById(productDTO.merchantId)) {
            log.error("createProduct: merchant does not exist")
            return GenericResponse.ErrorResponse("Comercio no existe")
        }
        if (saveImage(
                multipartFile,
                productDTO
            )
        ) return GenericResponse.ErrorResponse("La imagén debe tener al menos 800 px")

        val product = productRepository.save(productDTO.toEntity())
        return GenericResponse.SuccessResponse(product.toDTO())
    }

    private fun saveImage(
        multipartFile: MultipartFile?,
        productDTO: ProductDTO
    ): Boolean {
        if (multipartFile != null) {
            val file = convertMultiPartToFile(multipartFile)
            val iis = ImageIO.createImageInputStream(file)
            val imageReaders = ImageIO.getImageReaders(iis).next()
            val bufferedImage = ImageIO.read(FileInputStream(file))
            if (bufferedImage.width < PRODUCT_WIDTH) {
                return true
            }
            val compressedFile = compressImage(bufferedImage, multipartFile, imageReaders, PRODUCT_WIDTH)
            val compressedFileMin = compressImage(
                bufferedImage, multipartFile, imageReaders, PRODUCT_WIDTH_MIN,
                "${productDTO.name}_min"
            )
            val imageURL = amazonClient.uploadFile(compressedFile, productDTO.merchantId.toString())
            val imageURLMin = amazonClient.uploadFile(compressedFileMin, productDTO.merchantId.toString())
            productDTO.imageUrl = imageURL
            productDTO.imageUrlMin = imageURLMin
        }
        return false
    }

    fun save(product:Product)= productRepository.save(product).toDTO()

    fun updateProduct(productRequest: UpdateProductRequest, token: String,file:MultipartFile?): GenericResponse<*> {

        val userName = tokenProvider.getUsername(token)
        val merchantList = merchantRepository.findByEmail(userName)
        if (merchantList.isEmpty()) {
            log.error("updateProduct: merchant does not exist")
            return GenericResponse.ErrorResponse("Merchant in session does not exist")
        }
        val merchantIdInSession = merchantList[0].id
        val productList = getProductByShortId(productRequest.shortId)
        if (productList.isEmpty()) {
            log.error("updateProduct: product does not exist")
            return GenericResponse.ErrorResponse("product to update does not exist")
        }
        val product = productList[0]
        if (product.merchantId != merchantIdInSession) {
            log.error("updateProduct: user can not update this product,it is not the owner")
            return GenericResponse.ErrorResponse("User can not update this product, it is not the owner")
        }
        val productDTO = product.toDTO()
        productDTO.amount = productRequest.amount
        productDTO.description = productRequest.description
        productDTO.name = productRequest.name
        productDTO.dropshipping = productRequest.dropshipping
        productDTO.dropshippingPrice = productRequest.dropshippingPrice

        if (saveImage(
                file,
                productDTO
            )
        ) return GenericResponse.ErrorResponse("La imagén debe tener al menos 800 px")
        productRepository.save(productDTO.toEntity())

        return GenericResponse.SuccessResponse(product.toDTO())
    }

    fun updateInventoryProduct(updateRequest: UpdateInventoryProductRequest, token: String): GenericResponse<*> {

        val userName = tokenProvider.getUsername(token)
        val merchantList = merchantRepository.findByEmail(userName)
        if (merchantList.isEmpty()) {
            log.error("updateProduct: merchant does not exist")
            return GenericResponse.ErrorResponse("Merchant in session does not exist")
        }
        val merchantIdInSession = merchantList[0].id
        val optionalProduct = findById(updateRequest.id)
        if (optionalProduct.isEmpty) {
            log.error("updateProduct: product does not exist")
            return GenericResponse.ErrorResponse("producto no existe")
        }
        val product = optionalProduct.get()
        if (product.merchantId != merchantIdInSession) {
            log.error("updateProduct: user can not update this product,it is not the owner")
            return GenericResponse.ErrorResponse("No puedes actualizar un producto que no es tuyo")
        }
        product.inventory = updateRequest.inventory
        productRepository.save(product)
        return GenericResponse.SuccessResponse(product.toDTO())
    }

    fun createPaymentFromProduct(paymentAgree: PaymentAgree, amount: BigDecimal = BigDecimal.ZERO,merchantId: UUID?=null): GenericResponse<*> {
        val productList = getProductByShortId(paymentAgree.idPayment!!)
        if (productList.isEmpty()) {
            log.error("product do not exist")
            GenericResponse.ErrorResponse("Product does not exist")
        }
        val product = productList[0]
        val payment = PaymentDTO()
        payment.amount = if (amount > BigDecimal.ZERO) {
            amount
        } else {
            product.amount
        }
        payment.description = product.name?:product.description
        payment.idMerchant = merchantId?.let { it }?:product.merchantId
        payment.productId = product.id
        val paymentDTOWrapped = paymentService.createPayment(payment, true)
        val finalPayment = paymentDTOWrapped.objP as PaymentDTO
        paymentAgree.idPayment = finalPayment.id.toString()
        paymentService.agreePayment(paymentAgree)
        return GenericResponse.SuccessResponse(finalPayment.id.toString().takeLast(6))
    }

    fun removeProductsById(ids: List<String>): GenericResponse<*> {
        val products = arrayListOf<Product>()
        ids.forEach {
            val productList = getProductByShortId(it)
            if (productList.isEmpty()) {
                log.error("error deleting product, product not found id: $it")
            } else {
                val product = productList[0]
                deleteAssociatedInformation(product)
                product.disabled = true
                product.deletionDate = LocalDateTime.now()
                products.add(product)
            }
        }
        removeProducts(products)
        return GenericResponse.SuccessResponse("Deleted successfully")
    }

    private fun deleteAssociatedInformation(product: Product) {
        val deletionDate = LocalDateTime.now()
        val provider = merchantRepository.findById(product.merchantId)
        dropshippingSaleRepository.findByProductId(product.id).forEach { dropshippingSale ->
            dropshippingSale.disabled = true
            dropshippingSale.deletionDate = deletionDate
            dropshippingSaleRepository.save(dropshippingSale)
            val merchant = dropshippingSale.merchant
            emailSender.sendEmailWithTemplate(receiver = merchant.email, templateName = TEMPLATE_EMAIL_PLANE_TEXT, title = SUBJECT_PRODUCT_REMOVED,
                    o = getParamsForProductDeleted(productName = product.name
                            ?: "Sin nombre", providerName = provider.get().name))
        }
    }

    fun removeProducts(products: List<Product>) {
        productRepository.saveAll(products)
    }

    fun getProductsByMerchant(merchantId: UUID) = productRepository.findByMerchantIdAndDisabledFalseOrderByDescriptionAsc(merchantId).map {
        it.toDTO()
    }

    fun deleteImage(name: String): String {
        return amazonClient.deleteFileFromS3Bucket(name)
    }

    fun getProductByCriteria(searchCriteriaItems: List<SearchCriteria>?, token: String): List<ProductDTO> {
        val genericSpecification = GenericSpecification<Product>()
        searchCriteriaItems?.forEach {
            genericSpecification.add(SearchCriteria(it.key, it.value, it.operation))
        }
        val userName = tokenProvider.getUsername(token)
        val merchantInSession = merchantRepository.findByEmail(userName)
        if (merchantInSession.isEmpty()) {
            GenericResponse.ErrorResponse("User does not exist")
        }
        val merchantIdInSession = merchantInSession[0].id
        val allProducts = productRepository.findAll(genericSpecification)
        val dropProducts = allProducts.filter {
            it.dropshipping == true
        }.map { it.toDTO() }
        val nonDropProducts = allProducts.filter {
            it.dropshipping == false && it.merchantId == merchantIdInSession
        }.map { it.toDTO() }
        return dropProducts + nonDropProducts
    }

    fun getProductByShortId(id: String): List<Product> {
        return if (id == null || id.isBlank()) {
            listOf()
        } else {
            productRepository.getProductByShortId(id)
        }
    }

    fun getById(id: UUID) = productRepository.findById(id)

    fun getParamsForProductDeleted(productName: String, providerName: String): Map<String, String> {
        val message = "El proveedor $providerName eliminó el producto $productName. Por esta razón nuestro sistema no " +
                "te permitirá seguir usando el checkout para este producto. Si tienes campañas activas para este, " +
                "te recomendamos detenerlas."
        return mapOf(CONST_MESSAGE to message,
                CONST_TITLE_BODY to "Uno de tus proveedores eliminó uno de los productos que tienes vinculados")
    }

    fun getDropProductsForMerchant(merchantId: UUID) = productRepository.findByMerchantIdAndDropshippingIsTrue(merchantId)

    fun findById(productId: UUID) = productRepository.findById(productId)

    fun findByIdOrNull(productId: UUID) = productRepository.findByIdOrNull(productId)

    fun existsById(productId: UUID) = productRepository.existsById(productId)
}