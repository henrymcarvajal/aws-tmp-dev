package com.mps.payment.core.repository.criteria

import org.springframework.data.jpa.domain.Specification
import java.util.*
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

class GenericSpecification<T> : Specification<T> {

    private val list: MutableList<SearchCriteria>

    fun add(criteria: SearchCriteria) {
        list.add(criteria)
    }

    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, builder: CriteriaBuilder): Predicate? {

        //create a new predicate list
        val predicates: MutableList<Predicate> = ArrayList()

        //add criteria to predicates
        for (criteria in list) {
            if (criteria.operation == SearchOperation.EXISTENT) {
                predicates.add(
                    builder.isNotNull(
                        root.get<T>(criteria.key)
                    )
                )
            } else if (criteria.operation == SearchOperation.NON_EXISTENT) {
                predicates.add(
                    builder.isNull(
                        root.get<T>(criteria.key)
                    )
                )
            } else if (criteria.operation == SearchOperation.GREATER_THAN) {
                predicates.add(
                    builder.greaterThan(
                        root.get(criteria.key), criteria.value.toString()
                    )
                )
            } else if (criteria.operation == SearchOperation.LESS_THAN) {
                predicates.add(
                    builder.lessThan(
                        root.get(criteria.key), criteria.value.toString()
                    )
                )
            } else if (criteria.operation == SearchOperation.GREATER_THAN_EQUAL) {
                predicates.add(
                    builder.greaterThanOrEqualTo(
                        root.get(criteria.key), criteria.value.toString()
                    )
                )
            } else if (criteria.operation == SearchOperation.LESS_THAN_EQUAL) {
                predicates.add(
                    builder.lessThanOrEqualTo(
                        root.get(criteria.key), criteria.value.toString()
                    )
                )
            } else if (criteria.operation == SearchOperation.NOT_EQUAL) {
                predicates.add(
                    builder.notEqual(
                        root.get<Any>(criteria.key), criteria.value
                    )
                )
            } else if (criteria.operation == SearchOperation.EQUAL) {
                predicates.add(
                    builder.equal(
                        root.get<Any>(criteria.key), criteria.value
                    )
                )
            } else if (criteria.operation == SearchOperation.MATCH) {
                predicates.add(
                    builder.like(
                        builder.lower(root.get(criteria.key)),
                        "%" + criteria.value.toString().toLowerCase() + "%"
                    )
                )
            } else if (criteria.operation == SearchOperation.MATCH_END) {
                predicates.add(
                    builder.like(
                        builder.lower(root.get(criteria.key)),
                        criteria.value.toString().toLowerCase() + "%"
                    )
                )
            }
        }
        return builder.and(*predicates.toTypedArray())
    }

    init {
        list = ArrayList()
    }
}