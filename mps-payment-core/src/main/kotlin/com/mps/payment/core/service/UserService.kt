package com.mps.payment.core.service

import com.mps.common.auth.request.IntegrationRequest
import com.mps.common.dto.GenericResponse
import com.mps.payment.core.client.partner.SecurityClient
import com.mps.payment.core.email.EmailSender
import com.mps.payment.core.model.User
import com.mps.payment.core.repository.MerchantRepository
import com.mps.payment.core.repository.UserRepository
import com.mps.payment.core.security.jwt.JwtTokenProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*





const val TEMPLATE_GENERIC_BUTTON="generic_title_text_button"
const val INTEGRATION_CREDENTIALS_TEMPLATE="Integration_Credentials"
const val SUBJECT_WELCOME="Bienvenido a MiPagoSeguro"
const val SUBJECT_RECOVERY="Crea una nueva contraseña"
const val SUBJECT_INTEGRATION="Tu información de integración"
const val MESSAGE_WELCOME="Por favor crea tu contraseña haciendo clic en el siguiente botón, para que puedas iniciar sesión. Si pasa más de una hora para la creación, deberás usar el flujo de recuperar contraseña para generarla."
const val MESSAGE_RECOVERY="Por favor crea una nueva contraseña haciendo clic en el siguiente botón. Si pasa más de una hora para la creación, deberás volver a recuperar tu contraseña"
@Service
class UserService (
        private val userRepository: UserRepository,
        private val emailSender: EmailSender,
        private var jwtTokenProvider: JwtTokenProvider,
        private val securityClient: SecurityClient,
        private val merchantRepository: MerchantRepository
) {

    @Value("\${fe.url}")
    lateinit var  url: String


    val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun create(user: User): User? {
        if (user.id == null) {
            val existingUser : Optional<User?> = userRepository.findByUsername(user.username)
            if (!existingUser.isPresent) {
                user.createdAt = LocalDateTime.now()
                val savedUser = userRepository.save(user)
                sendWelcomeOrRecoveryEmail(savedUser)
                return savedUser
            }
        }
        return null
    }

    fun generateIntegrationInformation(clientId:UUID):GenericResponse<String>{
        val integrationRequest = IntegrationRequest(grantType = "client_credentials",resource = "woo"
                ,merchantID = clientId.toString(),scopes = "woo.read")
        val response = securityClient.createIntegrationInfo(integrationRequest)
        val merchant = merchantRepository.findById(clientId)
        if(merchant.isEmpty){
            return GenericResponse.ErrorResponse("Comercio no existe")
        }
        response?.let {
            emailSender.sendEmailWithTemplate(templateName = INTEGRATION_CREDENTIALS_TEMPLATE, title = SUBJECT_INTEGRATION,
                    o = mapOf("integrationInfo" to listOf(KeyValue("ID_CLIENTE",it.clientId),
                            KeyValue("MPS_KEY",it.secret), KeyValue("MPS_PUBLIC_KEY",it.publicKey))),
                    receiver = merchant.get().email)
            return GenericResponse.SuccessResponse("Información enviado a correo electrónico")
        } ?: return GenericResponse.ErrorResponse("Fallo generando información por favor contactar administrador")

    }

    fun recoveryPassword(email:String): GenericResponse<*> {
        val emailFixed = email.replace("%40","@").replace("=","")
        log.info("email before $email email after $emailFixed")
        val user = userRepository.findByUsername(emailFixed)
        if(user.isEmpty){
            log.error("RecoveryPassword: user empty")
            return GenericResponse.ErrorResponse("Usuario no existe")
        }
        sendWelcomeOrRecoveryEmail(user.get(),true)
        return GenericResponse.SuccessResponse("Enviado")
    }

    fun update(user: User): User? {
        if (user.id != null) {
            user.updatedAt = LocalDateTime.now()
            return userRepository.save(user)
        }
        return null
    }

    fun delete(id: UUID) {
        val optionalUser = userRepository.findById(id)
        optionalUser.ifPresent { item: User ->
            item.disabled = true
            item.updatedAt = LocalDateTime.now()
            userRepository.save(item)
        }
    }

    fun findById(id: UUID): User? {
        val os = userRepository.findById(id)
        return os.orElse(null)
    }

    fun findByUsername(username: String): User? {
        val os = userRepository.findByUsername(username)
        return os.orElse(null)
    }

    private fun sendWelcomeOrRecoveryEmail(user: User, isRecoveryPass: Boolean = false) {
        log.info("sending email $user")
        val token = jwtTokenProvider.createToken(user.username, user.roles.split("."))
        val encoding = Base64.getUrlEncoder().encodeToString(token.toByteArray())
        val url = "${url}user/password/$encoding"
        if (isRecoveryPass) {
            emailSender.sendEmailWithTemplate(templateName = TEMPLATE_GENERIC_BUTTON, title = SUBJECT_RECOVERY,
                    o = mapOf(LINK to url, CONST_MESSAGE to MESSAGE_RECOVERY, "titleMessage" to SUBJECT_RECOVERY,
                            "buttonText" to "Crear Contraseña"),
                    receiver = user.username)
        } else {
            emailSender.sendEmailWithTemplate(templateName = TEMPLATE_GENERIC_BUTTON, title = SUBJECT_WELCOME,
                    o = mapOf(LINK to url, CONST_MESSAGE to MESSAGE_WELCOME, "titleMessage" to SUBJECT_WELCOME,
                            "buttonText" to "Crear Contraseña"),
                    receiver = user.username)
        }
    }
}
data class KeyValue(
        val name:String,
        val value:String
)