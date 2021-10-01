package com.mps.payment.core.client.entregalo

import com.mps.payment.core.client.entregalo.payload.*

interface EntregaloClient {
    fun sendFreightRequest(data: AskNewServiceRequestInput): AskNewServiceResponse
    fun sendQueryServiceStatusRequest(data: QueryStatusRequest): QueryServiceStatusResponse
    fun getCities(): List<CityDTO>?
    fun saveBranch(data: CreateBranchRequestInput):CreateBranchResponse
}

