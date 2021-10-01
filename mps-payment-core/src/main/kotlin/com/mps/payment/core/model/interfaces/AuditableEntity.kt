package com.mps.payment.core.model.interfaces

import java.time.LocalDateTime

interface AuditableEntity {
    var createdAt: LocalDateTime
    var updatedAt: LocalDateTime?
}