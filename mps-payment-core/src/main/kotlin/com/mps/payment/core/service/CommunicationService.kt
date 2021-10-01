package com.mps.payment.core.service

import com.mps.payment.core.client.sms.SendMessageRequestInput
import com.mps.payment.core.client.sms.SmsClient
import com.mps.payment.core.email.EmailSender
import org.springframework.stereotype.Service

@Service
class CommunicationService(private val emailSender: EmailSender,
                           private val smsClient: SmsClient) {
    fun sendSmSAndEmail(email:String,contactNumber:String,smsMessage:String,
                        url:String,template:String,title:String,emailMessage:String,subject:String,actionText:String){
        emailSender.sendEmailWithTemplate(receiver = email, templateName = template, title = title,
                o = mapOf(CONST_MESSAGE to emailMessage,
                        LINK to url, "headBody" to subject,
                        "actionText" to actionText))
        smsClient.sendSmsMessage(SendMessageRequestInput(number = contactNumber, message = smsMessage))
    }

    fun sendEmailWithTemplate(receiver: String, title: String, o: Any, templateName: String) = emailSender.sendEmailWithTemplate(
            receiver,title,o,templateName
    )
}