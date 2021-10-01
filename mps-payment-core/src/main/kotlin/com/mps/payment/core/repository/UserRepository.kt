package com.mps.payment.core.repository

import com.mps.payment.core.model.User
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional


@Repository
interface UserRepository: CrudRepository<User, UUID> {
    fun findByUsername(username: String) : Optional<User?>
    @Transactional
    @Query("UPDATE User u SET u.failedAttempts = ?1 WHERE u.username = ?2")
    @Modifying
    fun updateFailedAttempts(failAttempts: Int, email: String?)
}