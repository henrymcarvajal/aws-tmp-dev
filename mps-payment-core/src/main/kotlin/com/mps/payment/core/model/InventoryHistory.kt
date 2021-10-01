package com.mps.payment.core.model

import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class InventoryHistory(
    @Id var id: UUID,
    var inventoryId: UUID,
    var quantityBefore: Int,
    var quantityAfter: Int,
    var source: String,
    var operation: String,
    @Column(name = "created_at") var creationDate: LocalDateTime = LocalDateTime.now()
)

data class InventoryHistoryDTO(
    var id: UUID? = UUID.randomUUID(),
    var inventoryId: UUID,
    var quantityBefore: Int,
    var quantityAfter: Int,
    var source: String,
    var operation: String,
    var creationDate: LocalDateTime = LocalDateTime.now()
)

fun InventoryHistory.toDTO() = InventoryHistoryDTO(
    id = this.id,
    inventoryId = this.inventoryId,
    quantityBefore = this.quantityBefore,
    quantityAfter = this.quantityAfter,
    source = this.source,
    operation = this.operation,
    creationDate = this.creationDate
)

fun InventoryHistoryDTO.toEntity() = InventoryHistory(
    id = this.id ?: UUID.randomUUID(),
    inventoryId = this.inventoryId,
    quantityBefore = this.quantityBefore,
    quantityAfter = this.quantityAfter,
    source = this.source,
    operation = this.operation,
    creationDate = this.creationDate
)