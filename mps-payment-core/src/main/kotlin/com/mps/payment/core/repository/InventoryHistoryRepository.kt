package com.mps.payment.core.repository

import com.mps.payment.core.model.InventoryHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface InventoryHistoryRepository : JpaRepository<InventoryHistory, UUID>
