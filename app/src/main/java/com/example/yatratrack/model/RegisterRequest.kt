package com.example.yatratrack.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val fullName: String,
    val gender: String,
    val dob: String,
    val address: String,
    val nicNumber: String,
    val phoneNumber: String
)