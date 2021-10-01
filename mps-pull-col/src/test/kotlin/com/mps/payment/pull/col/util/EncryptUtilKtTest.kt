package com.mps.payment.pull.col.util

import org.junit.jupiter.api.Test

internal class EncryptUtilKtTest{

    @Test
    fun `generating signature happy path`(){
        val message = "INTERACTIVE+5124+TEST+840+PAYMENT+SINGLE+12345678+20170129130025+123456+V2+1122334455667788"
        val key = "1122334455667788"
        val signature = generateSignature(message,key)
        assert("EKrcj4e8N38LGCP/xkJMaHUajUfvsRG50mDwYLNBsMU=".equals(signature))
    }

    @Test
    fun `empty message`(){
        val message = ""
        val key = "1122334455667788"
        val signature = generateSignature(message,key)
        assert(null==signature)
    }

    @Test
    fun `empty key`(){
        val message = "INTERACTIVE+5124+TEST+840+PAYMENT+SINGLE+12345678+20170129130025+123456+V2+1122334455667788"
        val key = ""
        val signature = generateSignature(message,key)
        assert(null==signature)
    }

    @Test
    fun `empty key and message`(){
        val message = ""
        val key = ""
        val signature = generateSignature(message,key)
        assert(null==signature)
    }
}