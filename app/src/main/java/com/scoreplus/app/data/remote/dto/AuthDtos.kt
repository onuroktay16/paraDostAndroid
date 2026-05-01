package com.scoreplus.app.data.remote.dto

data class RegisterRequest(val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class RefreshRequest(val refreshToken: String)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: Int,
    val email: String
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)
