package com.mps.payment.core.email

import com.mps.payment.core.model.toEntity
import com.mps.payment.core.service.TEMPLATE_EMAIL_PLANE
import com.mps.payment.core.task.CLOSED_PAYMENT_TEMPLATE
import com.mps.payment.core.util.createPaymentTest
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner


@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest
internal class EmailSenderImplTest {

    @Autowired
    lateinit var emailSender:EmailSenderImpl


    @Test
    @Ignore
    fun sendEmailWithTemplateToSeller() {
        emailSender.sender="mipagoseguro.col@gmail.com"
        val model = HashMap<String, Any>()
        model["resultadoCosto"] = "$150.000"
        model["link"] = "http://linkprueba.com"
        emailSender.sendEmailWithTemplate(receiver = "jorjek4@hotmail.com",
                title = "esto es un correo de prueba",templateName = TEMPLATE_EMAIL_PLANE,o = model)
    }

    @Test
    @Ignore
    fun sendEmailClosedPaymentsTemplate() {
        emailSender.sender="mipagoseguro.col@gmail.com"
        val model = HashMap<String, Any>()
        model["message"] = "texto de prueba de mensaje"
        model["title_body"] = "titulo body de prueba"
        model["closedPayments"] = listOf(createPaymentTest().toEntity())
        emailSender.sendEmailWithTemplate(receiver = "jorjek4@hotmail.com",
                title = "esto es un correo de prueba",templateName = CLOSED_PAYMENT_TEMPLATE,o = model)
    }

    @Test
    @Ignore
    fun sendEmailWithTemplateToBuyer() {
        emailSender.sender="mipagoseguro.col@gmail.com"
        val model = HashMap<String, Any>()
        model["message"] = "Se ha generado un pago seguro por el valor de 150.000. Para realizar el cobro. Una vez el pago sea efectuado y nuestro sistema lo verifique, el vendedor procederá con el envio de tu producto."
        emailSender.sendEmailWithTemplate(receiver = "jorjek4@hotmail.com",
                title = "esto es un correo de prueba para el comprador",templateName = "plane_template",o = model)
    }
}