package com.example.yatratrack.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yatratrack.model.RegisterRequest
import com.example.yatratrack.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

class RegisterViewModel : ViewModel() {

    companion object {
        private const val TAG = "RegisterViewModel"
    }

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        // Add HTTP logging
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("HTTP_CLIENT", message)
                }
            }
            level = LogLevel.ALL
        }
    }

    private val repository = UserRepository(httpClient)

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState

    fun registerUserWithBackend(context: Context,user: RegisterRequest) {
        Log.d(TAG, "Starting backend registration process...")

        viewModelScope.launch {
            try {
                _registrationState.value = RegistrationState.Loading

                Log.d(TAG, "Getting Firebase auth token...")
                val currentUser = FirebaseAuth.getInstance().currentUser

                if (currentUser == null) {
                    Log.e(TAG, "No authenticated user found")
                    _registrationState.value = RegistrationState.Failure("No authenticated user")
                    return@launch
                }

                Log.d(TAG, "Current user UID: ${currentUser.uid}")

                val tokenResult = currentUser.getIdToken(false).await()
                val token = tokenResult?.token

                if (token != null) {
                    Log.d(TAG, "Token retrieved successfully, length: ${token.length}")
                    Log.d(TAG, "Calling repository registerUser...")

                    val backendToken = repository.loginAndGetBackendToken(token)

                    if (backendToken != null) {
                        // ✅ Save token
                        TokenManager.saveToken(context, backendToken)
                        val result = repository.registerUser(backendToken, user)

                        if (result != null) {
                            TokenManager.saveUserId(context, result.id) // ✅ Save user ID
                            _registrationState.value = RegistrationState.Success
                        } else {
                            _registrationState.value = RegistrationState.Failure("Backend registration failed")
                        }
                    } else {
                        Log.e(TAG, "Login step failed: backend token not received")
                        _registrationState.value = RegistrationState.Failure("Login failed, can't proceed to register")
                    }
                } else {
                    Log.e(TAG, "Failed to get ID token")
                    _registrationState.value = RegistrationState.Failure("Failed to get authentication token")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception in registerUserWithBackend", e)
                Log.e(TAG, "Exception details: ${e::class.java.simpleName} - ${e.message}")
                _registrationState.value = RegistrationState.Failure("Registration error: ${e.message}")
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, closing HTTP client")
        httpClient.close()
    }
}

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    object Success : RegistrationState()
    data class Failure(val message: String) : RegistrationState()
}