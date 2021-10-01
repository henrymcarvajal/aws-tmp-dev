package com.mps.payment.core.client.adapter

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;



class ObjectToUrlEncodedConverter(mapper: ObjectMapper) : HttpMessageConverter<Any?> {
    private val mapper: ObjectMapper

    private class UrlEncodedWriter {
        private val out = StringBuilder()
        @JsonAnySetter
        @Throws(UnsupportedEncodingException::class)
        fun write(name: String?, property: Any?) {
            if (out.length > 0) {
                out.append("&")
            }
            out
                    .append(URLEncoder.encode(name, Encoding))
                    .append("=")
            if (property != null) {
                out.append(URLEncoder.encode(property.toString(), Encoding))
            }
        }

        override fun toString(): String {
            return out.toString()
        }
    }

    companion object {
        private const val Encoding = "UTF-8"
    }

    init {
        this.mapper = mapper
    }

    override fun canRead(p0: Class<*>, p1: MediaType?): Boolean {
        return false
    }

    override fun canWrite(p0: Class<*>, mediaType: MediaType?): Boolean {
        return supportedMediaTypes.contains(mediaType)
    }

    override fun write(o: Any, contentType: MediaType?, outputMessage: HttpOutputMessage) {
        if (o != null) {
            val body: String = mapper
                    .convertValue(o, UrlEncodedWriter::class.java)
                    .toString()
            try {
                outputMessage.body.write(body.toByteArray(charset(Encoding)))
            } catch (e: IOException) {
                // if UTF-8 is not supporter then I give up
            }
        }
    }

    override fun read(p0: Class<out Any?>, p1: HttpInputMessage): Any {
        throw RuntimeException()
    }

    override fun getSupportedMediaTypes(): MutableList<MediaType> {
        return Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED)
    }
}
