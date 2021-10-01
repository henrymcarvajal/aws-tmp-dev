package com.mps.payment.core.repository

import com.mps.payment.core.model.DropshippingSale
import com.mps.payment.core.model.GeneralOrderDrop
import java.time.LocalDateTime
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.TypedQuery
import javax.persistence.criteria.*


interface OrderRepositoryCustom {
    fun findOrdersByProductsAndtDynamicParams(products: List<UUID>,
                                              limitDate: LocalDateTime?,
                                              orderState: Int?, guideNumber: Int?,
                                              pageNumber: Int, customerId: UUID?, pageSize: Int): OrderRepositoryCustomImpl.PageInformation
}

class OrderRepositoryCustomImpl : OrderRepositoryCustom {


    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun findOrdersByProductsAndtDynamicParams(products: List<UUID>,
                                                       limitDate: LocalDateTime?,
                                                       orderState: Int?, guideNumber: Int?,
                                                       pageNumber: Int, customerId: UUID?, pageSize: Int): PageInformation {

        val cb: CriteriaBuilder = entityManager!!.criteriaBuilder
        val query: CriteriaQuery<GeneralOrderDrop> = cb.createQuery(GeneralOrderDrop::class.java)
        val order: Root<GeneralOrderDrop> = query.from(GeneralOrderDrop::class.java)
        val dropshippingSaleJoin:Join<GeneralOrderDrop, DropshippingSale> =order.join("dropShippingSale")
        val predicates: MutableList<Predicate> = ArrayList()
        val predicatesToCount: MutableList<Predicate> = ArrayList()

        val productIdsPredicate = dropshippingSaleJoin.get<UUID>("id").`in`(products)
        predicates.add(cb.and(productIdsPredicate))

        if (customerId != null) {
            val generalOrderCustomerId: Path<String> = order.get("customerId")
            val predicateCustomer = cb.equal(generalOrderCustomerId, customerId)
            predicates.add(predicateCustomer)
            predicatesToCount.add(predicateCustomer)
        }

        if (limitDate != null) {
            val createdDate: Path<String> = order.get("creationDate")
            val predicateDate = cb.greaterThan(createdDate.`as`(LocalDateTime::class.java), limitDate)
            predicates.add(predicateDate)
            predicatesToCount.add(predicateDate)
        }
        if (orderState != null && orderState > 0) {
            val generalOrderState: Path<String> = order.get("orderStatus")
            val predicateOrder = cb.equal(generalOrderState, orderState)
            predicates.add(predicateOrder)
            predicatesToCount.add(predicateOrder)
        }
        guideNumber?.let {
            val generalOrderGuideNumber: Path<String> = order.get("guideNumber")
            val predicateGuideNumber = cb.equal(generalOrderGuideNumber, guideNumber)
            predicates.add(predicateGuideNumber)
            predicatesToCount.add(predicateGuideNumber)
        }
        val generalOrderBranchCode: Path<Int> = order.get("branchCode")
        val predicateBranch = cb.notEqual(generalOrderBranchCode, 0)
        predicates.add(predicateBranch)
        predicatesToCount.add(predicateBranch)

        query.select(order)
                .where(cb.and(*predicates.toTypedArray())).orderBy(cb.desc(order.get<LocalDateTime>("creationDate")))

        return paginateQuery(predicatesToCount, entityManager.createQuery(query), pageNumber, pageSize,products)
    }

    private fun paginateQuery(predicates: MutableList<Predicate>, querie: TypedQuery<GeneralOrderDrop>, pageNumber: Int,
                              pageSize: Int,products: List<UUID>): PageInformation {
        val cb = entityManager.criteriaBuilder
        val countQuery: CriteriaQuery<Long> = cb.createQuery(Long::class.java)
        val order = countQuery.from(GeneralOrderDrop::class.java)

        val dropshippingSaleJoin:Join<GeneralOrderDrop, DropshippingSale> =order.join("dropShippingSale")
        val productIdsPredicate = dropshippingSaleJoin.get<UUID>("id").`in`(products)
        predicates.add(cb.and(productIdsPredicate))

        countQuery.select(cb.count(order))
        countQuery.where(cb.and(*predicates.toTypedArray()))
        val totalRecords = entityManager.createQuery(countQuery).singleResult
        return if (totalRecords != null && totalRecords > 0) {
            val initialIndex = (pageNumber * pageSize) - pageSize
            val pageNumberRecalc = ((totalRecords / pageSize) + 1).toInt()

            if (initialIndex + 1 <= totalRecords) {
                querie.firstResult = initialIndex
                querie.maxResults = pageSize
                PageInformation(querie.resultList, pageNumber, totalRecords.toInt(), pageNumberRecalc);
            } else {
                PageInformation(listOf(), pageNumber, totalRecords.toInt(), pageNumberRecalc);
            }
        } else {
            PageInformation(listOf(), pageNumber, 0, 0);
        }
    }

    data class PageInformation(var records: List<GeneralOrderDrop>, val pageNumber: Int, val totalRecords: Int, val totalPages: Int)
}