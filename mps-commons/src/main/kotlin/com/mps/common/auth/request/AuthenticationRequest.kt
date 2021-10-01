package com.mps.common.auth.request

data class AuthenticationRequest(
    val username: String,
    val password: String)

data class IntegrationRequest(
        val resource:String,
        val merchantID:String,
        val scopes:String,
        val grantType:String
)

data class IntegrationInformation(
        val clientId:String,
        val publicKey:String,
        val secret:String
)