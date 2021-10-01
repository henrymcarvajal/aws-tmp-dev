package com.mps.payment.core.repository

import com.mps.payment.core.model.Branch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BranchRepository : JpaRepository<Branch, UUID>, JpaSpecificationExecutor<Branch> {

    fun findByMerchantIdAndDisabledFalseOrderByCreationDateAsc(merchantId: UUID): List<Branch>

    fun findByBranchCodeAndDisabledFalse(branchCode: Int): Optional<Branch>
}
