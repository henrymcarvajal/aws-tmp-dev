package com.mps.payment.core.security.service

import com.mps.payment.core.model.User
import com.mps.payment.core.repository.UserRepository
import org.springframework.stereotype.Component

import java.time.LocalDateTime
import java.time.ZoneId

import java.time.ZonedDateTime

@Component
class LockUserManager(private val repo: UserRepository) {


    private val LOCK_TIME_DURATION = (24 * 60 * 60 * 1000 // 24 hours
            ).toLong()
    val MAX_FAILED_ATTEMPTS = 5



    fun increaseFailedAttempts(user: User) {
        val newFailAttempts: Int = user.failedAttempts.plus(1)
        repo.updateFailedAttempts(newFailAttempts, user.username)
    }

    fun resetFailedAttempts(email: String?) {
        repo.updateFailedAttempts(0, email)
    }

    fun lock(user: User) {
        user.accountNonLocked=false
        user.lockTime= LocalDateTime.now()
        repo.save(user)
    }

    fun unlockWhenTimeExpired(user: User): Boolean {
        val zdt: ZonedDateTime = user.lockTime!!.atZone(ZoneId.systemDefault())
        val lockTimeInMillis: Long = zdt.toInstant().toEpochMilli()
        val currentTimeInMillis = System.currentTimeMillis()
        if (lockTimeInMillis + LOCK_TIME_DURATION < currentTimeInMillis) {
            user.accountNonLocked = true
            user.lockTime =null
            user.failedAttempts = 0
            repo.save(user)
            return true
        }
        return false
    }
}