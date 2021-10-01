package com.mps.payment.core.util.client

import org.apache.commons.codec.binary.Base64
import org.springframework.http.HttpHeaders
import java.nio.charset.Charset

const val DEFAULT_CHARSET = "US-ASCII"

fun createHeaders(userName: String, password: String): HttpHeaders {
    return object : HttpHeaders() {
        init {
            val auth = "$userName:$password"
            val encodedAuth: ByteArray = Base64.encodeBase64(
                    auth.toByteArray(Charset.forName(DEFAULT_CHARSET)))
            val authHeader = "Basic " + String(encodedAuth)
            set("Authorization", authHeader)
        }
    }
}

fun createTokenHeader(token: String): HttpHeaders {
    return with(HttpHeaders()) {
        val authHeader = "Bearer $token"
        set("Authorization", authHeader)
        this
    }
}