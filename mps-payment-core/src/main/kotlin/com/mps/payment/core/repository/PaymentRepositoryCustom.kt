package com.mps.payment.core.repository

import com.mps.payment.core.model.Payment
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.criteria.*


interface PaymentRepositoryCustom{
    fun findPaymentsPerMerchantDynamicParams(merchantId: UUID,  limitDate: LocalDateTime?, paymentState:Int?):List<Payment>
}

class PaymentRepositoryCustomImpl:PaymentRepositoryCustom{


    @PersistenceContext
    private val entityManager: EntityManager? = null

    override fun findPaymentsPerMerchantDynamicParams(merchantId: UUID, limitDate: LocalDateTime?, paymentState:Int?):List<Payment> {

        val cb: CriteriaBuilder = entityManager!!.criteriaBuilder
        val query: CriteriaQuery<Payment> = cb.createQuery(Payment::class.java)
        val payment: Root<Payment> = query.from(Payment::class.java)

        val predicates: MutableList<Predicate> = ArrayList()

        val merchantIdPredicate = payment.get<String>("idMerchant")
        predicates.add(cb.equal(merchantIdPredicate,merchantId))

        if(limitDate!=null){
            val createdDate: Path<String> = payment.get("creationDate")
            predicates.add(cb.greaterThan(createdDate.`as`(LocalDateTime::class.java),limitDate))
        }
        if(paymentState!=null){
            val idpaymentState: Path<String> = payment.get("idState")
            predicates.add(cb.equal(idpaymentState,paymentState))
        }

        query.select(payment)
                .where(cb.and(*predicates.toTypedArray())).orderBy(cb.desc( payment.get<LocalDateTime>("creationDate")))

        return entityManager.createQuery(query)
                .resultList
    }

}