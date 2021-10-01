package com.mps.payment.core.client.entregalo.payload

import com.mps.payment.core.client.entregalo.payload.queryservicestatus.external.ExternalQueryServiceStatusData
import com.mps.payment.core.client.entregalo.payload.queryservicestatus.external.ExternalQueryServiceStatusResponse
import org.springframework.http.HttpStatus

data class CreateBranchRequestInput(
    val title: String,
    val address: String,
    val peopleName: String,
    val peoplePhone: Long,
    val peopleEmail:String,
    val peopleIdentification:Long
)

data class CreateBranchResponse(
    val status:HttpStatus,
    val branchName: String? = null,
    val branchCode: Int = 0
)

data class EntregaloNewBranchResponse(
    val error: Boolean,
    val messages: Array<String>,
    val data: EntregaloNewBranchResponseData
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EntregaloNewBranchResponse

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

data class EntregaloNewBranchResponseData(
    val id: Int,
    val title:String
)