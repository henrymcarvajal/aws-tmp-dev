package com.mps.payment.core.client.entregalo.payload

data class GetCitiesResponse(
        val error:Boolean,
        val messages:Array<String>,
        val data: Cities
)

data class Cities(
        val Ciudades:List<Any>
)

data class CityDTO(
        val code:String,
        val city:String,
        val cityExtended:String?,
        val state:String,
        val idState:Int?=1,
        val againstDelivery:String
)