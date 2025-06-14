package com.example.yatratrack.model

@kotlinx.serialization.Serializable
data class LoginResponse(
    val jwtToken: String
)