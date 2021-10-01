package com.mps.payment.core.client.entregalo.payload.queryservicestatus.external

data class ExternalQueryServiceStatusResponse(
    val error: Boolean,
    val messages: Array<String>,
    val data: ExternalQueryServiceStatusData
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExternalQueryServiceStatusResponse

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