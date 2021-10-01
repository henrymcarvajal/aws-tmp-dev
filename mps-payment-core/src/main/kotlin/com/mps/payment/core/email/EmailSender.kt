package com.mps.payment.core.email

import com.mps.common.dto.GenericResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.io.File


interface EmailSender {
    fun sendEmailWithTemplate(receiver: String, title: String, o: Any, templateName: String): GenericResponse<*>
    fun sendEmailWithTemplateAndAttachments(recipient: String, title: String, data: MutableMap<String, Any>?, templateName: String, attachments: Map<String, File>): GenericResponse<*>
}

@Component
class EmailSenderImpl(private var mailSender: JavaMailSender, private var templateEngine: TemplateEngine) : EmailSender {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @Value("\${email.sender}")
    lateinit var sender: String

    override fun sendEmailWithTemplate(receiver: String, title: String, o: Any, templateName: String): GenericResponse<*> {
        try {
            logger.info("Start sending mail service,To:$receiver")
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true)
            helper.setFrom(sender)
            helper.setTo(receiver)
            helper.setSubject(title)

            val context = Context()
            context.setVariable("title", title)
            context.setVariables(o as MutableMap<String, Any>?)

            val content = templateEngine.process(templateName, context)
            helper.setText(content, true)

            mailSender.send(message)
            logger.info("End of mail delivery")
            return GenericResponse.SuccessResponse("Email sent")
        } catch (e: Exception) {
            logger.error("error sending email for recipient " +
                    "$receiver with template $templateName, exception ${e.cause}",e)
            return GenericResponse.ErrorResponse("Error sending email")
        }
    }

    override fun sendEmailWithTemplateAndAttachments(recipient: String, title: String, data: MutableMap<String, Any>?, templateName: String, attachments: Map<String, File>): GenericResponse<*> {
        try {
            logger.info("Start sending mail to: $recipient")
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true)
            helper.setFrom(sender)
            helper.setTo(recipient)
            helper.setSubject(title)

            val context = Context()
            context.setVariable("title", title)
            context.setVariables(data)

            val content = templateEngine.process(templateName, context)
            helper.setText(content, true)

            attachments.forEach { (k, v) -> helper.addAttachment(k, v) }

            mailSender.send(message)
            logger.info("End of mail delivery")
            return GenericResponse.SuccessResponse("Email sent")
        } catch (e: Exception) {
            logger.error("error sending email for recipient " +
                    "$recipient with template $templateName, exception ${e.cause}",e)
            return GenericResponse.ErrorResponse("Error sending email")
        }
    }
}