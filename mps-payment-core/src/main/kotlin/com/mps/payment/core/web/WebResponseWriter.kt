package com.mps.payment.core.web

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.io.IOException
import java.time.LocalDateTime
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletResponse

@Component
class WebResponseWriter {

    @Throws(IOException::class)
    fun writeResponse(response: ServletResponse, webResponse: WebResponse) {
        val servletResponse = response as HttpServletResponse
        servletResponse.status = webResponse.status
        servletResponse.contentType = "application/json"
        servletResponse.characterEncoding = "UTF-8"
        webResponse.timestamp = LocalDateTime.now().toString()
        val objectMapper = ObjectMapper()
        servletResponse.writer.write(objectMapper.writeValueAsString(webResponse))
    }
}