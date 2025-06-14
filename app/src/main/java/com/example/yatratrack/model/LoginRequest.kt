package com.example.yatratrack.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationRequest(
    val userId: Int,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String
)