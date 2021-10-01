package com.mps.payment.web.service

import com.mps.common.dto.GenericResponse
import com.mps.payment.core.model.User
import com.mps.payment.core.security.jwt.JwtTokenProvider
import com.mps.payment.core.security.service.LockUserManager
import com.mps.payment.core.service.MerchantService
import com.mps.payment.core.service.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import java.util.HashMap


@Component
class AuthService(private val authenticationManager: AuthenticationManager,
                  private val jwtTokenProvider: JwtTokenProvider,
                  private var userService: UserService,
                  private val merchantService: MerchantService,
                  private val lockUserManager: LockUserManager
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun signin(userName: String, password: String): GenericResponse<*> {

        val username = userName.trim().toLowerCase()
        val user: User = userService.findByUsername(username)
                ?: throw UsernameNotFoundException("Username $username not found")

        return try {
            authenticationManager.authenticate(UsernamePasswordAuthenticationToken(username, password))
            if (user.isEnabled) {
                val token = jwtTokenProvider.createToken(username, user.roles.split("."))
                val model: MutableMap<String, String> = HashMap()
                model["username"] = username
                model["token"] = token
                onAuthenticationSuccess(user)
                val merchant = user.id?.let { merchantService.getMerchant(it) }
                if (merchant != null) {
                    if (merchant.isPresent) {
                        model["merchantId"] = merchant.get().id.toString()
                        model["merchantName"] = merchant.get().name
                    }
                }
                return GenericResponse.SuccessResponse(model)
            } else {
                val message = onAuthenticationFailure(user)
                logger.error(message)
                return GenericResponse.ErrorResponse(message)
            }
        } catch (e: AuthenticationException) {
            val message = onAuthenticationFailure(user)
            logger.error(message)
            return GenericResponse.ErrorResponse(message)
        }
    }

    fun onAuthenticationSuccess(user: User) {
        if (user.failedAttempts > 0) {
            lockUserManager.resetFailedAttempts(user.username)
        }
    }

    fun onAuthenticationFailure(user: User): String {
        return if (user.isEnabled && user.accountNonLocked) {
            if (user.failedAttempts < lockUserManager.MAX_FAILED_ATTEMPTS - 1) {
                lockUserManager.increaseFailedAttempts(user)
                "Credenciales invalidas"
            } else {
                lockUserManager.lock(user);
                "Tu cuenta ha sido bloqueada por 24 horas " +
                        "debido a que has intentado ingresar 5 veces."

            }
        } else if (!user.accountNonLocked) {
            return if (lockUserManager.unlockWhenTimeExpired(user)) {
                "Tu cuenta ha sido desbloqueada. Por favor intenta ingresar nuevamente"
            } else {
                "Tu cuenta esta bloqueada, debes esperar 24 horas para que sea desbloqueada o comunicarte con el administrador."
            }
        } else {
            "cuenta bloqueada"
        }
    }
}