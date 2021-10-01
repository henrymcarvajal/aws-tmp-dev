package com.mps.payment.core.security

import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.security.crypto.encrypt.TextEncryptor

const val PASS_ENC = "EncryInfopass,"
const val SALT = "5c0744940b5c369b"

fun createTextEncryption(): TextEncryptor = Encryptors.text(PASS_ENC,SALT)