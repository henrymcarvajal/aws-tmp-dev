package com.mps.payment.core.repository

import com.mps.payment.core.model.GeneralOrderDrop
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Repository
interface OrderRepository : PagingAndSortingRepository<GeneralOrderDrop, UUID>,OrderRepositoryCustom {

    fun getByPaymentId(
            @Param("paymentId") paymentId: String): Optional<GeneralOrderDrop>

    fun findByOrderStatusIsNotAndGuideNumberIsNotNull(orderStatus: Int): List<GeneralOrderDrop>

    fun findByIdIn(id: List<UUID>): List<GeneralOrderDrop>

    @Query(value = "SELECT g FROM GeneralOrderDrop AS g where g.dropShippingSale.product.merchantId = :merchantId " +
            "and g.isLabeled is FALSE and g.orderStatus=2 and g.guideNumber is not null and g.label <> ''")
    fun getLabelOrdersWithoutLabelPerMerchant(merchantId: UUID):List<GeneralOrderDrop>


    @Query(value = "SELECT g.orderStatus AS status, COUNT(g) as quantity, SUM(g.amount) AS totalAmount, "
            + "SUM(g.amount-g.comision-g.freightPrice-g.dropShippingSale.product.dropshippingPrice) AS totalProfit "
            + "FROM GeneralOrderDrop AS g where g.dropShippingSale.id in :productsId  " +
            " and DATE(g.creationDate) between DATE(:initialDate) and DATE(:finalDate) and g.branchCode <> 0 GROUP BY g.orderStatus")
    fun getConsolidateByIntervalDateGroupByStatus(productsId: List<UUID>,initialDate:LocalDate, finalDate:LocalDate): List<IOrderConsolidateBySatus>

    @Query(value = "SELECT  g.orderStatus AS status, COUNT(g) as quantity, SUM(g.amount) AS totalAmount, "
            + "SUM(g.amount-g.comision-g.freightPrice-g.dropShippingSale.product.dropshippingPrice) AS totalProfit "
            + "FROM GeneralOrderDrop AS g where g.dropShippingSale.id in :productsId  " +
            " and DATE(g.creationDate)= DATE(:date) and g.branchCode <> 0 GROUP BY g.orderStatus")
    fun getConsolidateBySingleDateGroupByStatus(productsId: List<UUID>,date: LocalDate): List<IOrderConsolidateBySatus>
}

interface IOrderConsolidateBySatus {
    fun getStatus(): Int
    fun getQuantity(): Int
    fun getTotalAmount():BigDecimal
    fun getTotalProfit():BigDecimal
}