package com.mps.payment.pull.col.util

import java.nio.charset.Charset
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.util.Base64

fun generateSignature(message: String, secretKey: String): String? {

    if (message.isEmpty() || secretKey.isEmpty()) {
        return null
    }
    var hmacSha256 = try {
        Mac.getInstance("HmacSHA256");
    } catch (nsae: NoSuchAlgorithmException) {
        Mac.getInstance("HMAC-SHA-256");
    }
    val secretKeySpec = SecretKeySpec(secretKey.toByteArray(Charset.forName("UTF-8")), "HmacSHA256");
    hmacSha256.init(secretKeySpec);

    return Base64.getEncoder().encodeToString(hmacSha256.doFinal(message.toByteArray(Charset.forName("UTF-8"))))
}
