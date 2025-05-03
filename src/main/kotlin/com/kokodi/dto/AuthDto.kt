package com.kokodi.dto

data class RegisterRequest(
    val username: String,
    val password: String,
    val name: String
)

data class AuthRequest(
    val username: String,
    val password: String
)

data class AuthResponse(
    val token: String
) 