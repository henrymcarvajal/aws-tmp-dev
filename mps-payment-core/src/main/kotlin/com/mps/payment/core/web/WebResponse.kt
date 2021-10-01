package com.mps.payment.core.web

data class WebResponse(
        val type: String,
        val title: String,
        val detail: String,
        val status: Int
) {
    var timestamp: String? = null
}