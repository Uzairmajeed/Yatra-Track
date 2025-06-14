package com.example.yatratrack.repository

import android.util.Log
import com.example.yatratrack.model.LocationRequest
import com.example.yatratrack.model.LoginResponse
import com.example.yatratrack.model.RegisterRequest
import com.example.yatratrack.model.RegisterResponse
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserRepository(
    private val client: HttpClient
) {
    companion object {
        private const val TAG = "UserRepository"

        // ngrok URL (without trailing slash)
        private const val BASE_URL = "https://5961-137-59-0-53.ngrok-free.app"
    }



    suspend fun loginAndGetBackendToken(firebaseToken: String): String? {
        return try {
            Log.d(TAG, "Logging in with Firebase token...")
            val response: HttpResponse = client.post("$BASE_URL/api/auth/login") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                headers {
                    append("ngrok-skip-browser-warning", "true")
                }
                setBody(mapOf("firebaseToken" to firebaseToken))
            }

            val body = response.bodyAsText()
            Log.d(TAG, "Login response: $body")

            if (response.status.isSuccess()) {
                val parsed = Json.decodeFromString<LoginResponse>(body)
                Log.d(TAG, "Extracted JWT: ${parsed.jwtToken.take(15)}...")
                parsed.jwtToken
            } else {
                Log.e(TAG, "Login failed with status: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login exception occurred", e)
            null
        }
    }

    suspend fun registerUser(token: String, request: RegisterRequest): RegisterResponse? {
        return try {
            Log.d(TAG, "Starting user registration...")

            val response: HttpResponse = client.post("$BASE_URL/api/users/register") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                    append("ngrok-skip-browser-warning", "true")
                }
                setBody(request)
            }

            val responseBody = response.bodyAsText()
            Log.d(TAG, "Response body: $responseBody")

            if (response.status.isSuccess()) {
                val parsed = Json.decodeFromString<RegisterResponse>(responseBody)
                Log.d(TAG, "Parsed user ID: ${parsed.id}")
                parsed
            } else {
                Log.e(TAG, "Registration failed with status: ${response.status}")
                Log.e(TAG, "Error response: $responseBody")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration exception occurred", e)
            null
        }
    }

    suspend fun saveLocationToBackend(token: String, locationRequest: LocationRequest): Boolean {
        return try {
            Log.d(TAG, "Saving location to backend...")
            Log.d(TAG, "Location data: ${Json.encodeToString(locationRequest)}")

            val response: HttpResponse = client.post("$BASE_URL/api/users/location") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                    append("ngrok-skip-browser-warning", "true")
                }
                setBody(locationRequest)
            }

            val responseBody = response.bodyAsText()
            Log.d(TAG, "Location save response status: ${response.status}")
            Log.d(TAG, "Location save response body: $responseBody")

            if (response.status.isSuccess()) {
                Log.d(TAG, "Location saved to backend successfully")
                true
            } else {
                Log.e(TAG, "Failed to save location to backend: ${response.status}")
                Log.e(TAG, "Error response: $responseBody")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception saving location to backend", e)
            Log.e(TAG, "Exception type: ${e::class.java.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")

            // Log specific exception types for debugging
            when (e) {
                is io.ktor.client.network.sockets.ConnectTimeoutException -> {
                    Log.e(TAG, "Connection timeout while saving location - check ngrok tunnel")
                }
                is io.ktor.client.network.sockets.SocketTimeoutException -> {
                    Log.e(TAG, "Socket timeout while saving location - server took too long")
                }
                is java.net.ConnectException -> {
                    Log.e(TAG, "Connection refused while saving location - check if ngrok tunnel is active")
                }
                is java.net.UnknownHostException -> {
                    Log.e(TAG, "Unknown host while saving location - check ngrok URL")
                }
            }

            false
        }
    }
}