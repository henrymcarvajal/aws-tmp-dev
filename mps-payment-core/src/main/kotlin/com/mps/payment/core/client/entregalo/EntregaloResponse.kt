package com.mps.payment.core.client.entregalo

data class EntregaloResponse(
    val error: Boolean,
    val messages: Array<String>,
    val data: EntregaloData
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EntregaloResponse

        if (error != other.error) return false
        if (!messages.contentEquals(other.messages)) return false
        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = error.hashCode()
        result = 31 * result + messages.contentHashCode()
        result = 31 * result + data.hashCode()
        return result
    }
}

data class EntregaloData(
        val Guia: String,
        val Etiqueta: String
)