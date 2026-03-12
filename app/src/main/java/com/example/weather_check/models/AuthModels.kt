package com.example.weather_check.models

// ============ REQUEST MODELS ============

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class SendOtpRequest(
    val otpToken: String
)

data class VerifyOtpRequest(
    val otpToken: String,
    val code: String
)

data class DevLoginRequest(
    val otpToken: String
)

// ============ RESPONSE MODELS ============

data class RegisterResponse(
    val message: String
)

// Step 1: Login response - returns otpToken
data class LoginResponse(
    val loginOk: Boolean,
    val username: String,
    val email: String,
    val otpToken: String
)

// Step 3: Send OTP response
data class SendOtpResponse(
    val otpSent: Boolean,
    val email: String
)

// Step 4: Verify OTP response - returns JWT token
data class VerifyOtpResponse(
    val token: String,
    val username: String
)

// Step 5: Dev Login response - returns JWT token (dev mode only)
data class DevLoginResponse(
    val token: String,
    val username: String,
    val devMode: Boolean = true
)

// ============ ERROR & UTILITY MODELS ============

data class ErrorResponse(
    val error: String,
    val attemptsLeft: Int? = null
)

data class User(
    val id: String,
    val username: String
)

