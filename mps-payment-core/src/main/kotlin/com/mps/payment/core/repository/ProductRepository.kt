package com.mps.payment.core.repository

import com.mps.payment.core.model.Payment
import com.mps.payment.core.model.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext


@Repository
interface ProductRepository : JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    @Query(value = "select * from Product p where CAST(p.id AS text)  LIKE %:productId%",nativeQuery = true)
    fun getProductByShortId(
            @Param("productId") productId: String): List<Product>

    fun findByMerchantIdAndDisabledFalseOrderByDescriptionAsc(merchantId:UUID):List<Product>

    fun findByMerchantIdAndDropshippingIsTrue(merchantId:UUID):List<Product>
}
