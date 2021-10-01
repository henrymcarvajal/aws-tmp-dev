package com.mps.payment.core.repository

import com.mps.payment.core.model.DropshippingSale
import com.mps.payment.core.model.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface DropshippingSaleRepository : CrudRepository<DropshippingSale, UUID> {

    @Query(value = "SELECT d FROM DropshippingSale d WHERE d.merchant.id = :sellerMerchantId")
    fun findBySellerMerchantId(sellerMerchantId:UUID):List<DropshippingSale>

    @Query(value = "SELECT d FROM DropshippingSale d WHERE d.merchant.id = :sellerMerchantId and d.disabled=false")
    fun findBySellerMerchantIdAndDisabledFalse(sellerMerchantId:UUID):List<DropshippingSale>

    @Query(value = "SELECT d FROM DropshippingSale d WHERE d.product.id in :productId and d.disabled=false")
    fun findByProductIdInAndDisabledFalse(productId:List<UUID>):List<DropshippingSale>

    fun findByIdEqualsAndDisabledFalse(id:UUID):List<DropshippingSale>

    @Query(value = "SELECT d FROM DropshippingSale d WHERE d.product.id = :productId and d.merchant.id=:sellerMerchantId")
    fun findByProductIdAndSellerMerchantId(productId:UUID, sellerMerchantId:UUID):List<DropshippingSale>

    @Query(value = "SELECT d FROM DropshippingSale d WHERE d.product.id = :productId")
    fun findByProductId(productId:UUID):List<DropshippingSale>
}
