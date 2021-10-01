package com.mps.payment.core.util.exception

import java.io.PrintWriter
import java.io.StringWriter

class ExceptionUtils {

    companion object {
        fun toString(e: Throwable): String {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            return sw.toString()
        }
    }
}