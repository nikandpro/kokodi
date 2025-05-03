package com.kokodi.service

import com.kokodi.domain.User
import com.kokodi.dto.AuthRequest
import com.kokodi.dto.AuthResponse
import com.kokodi.dto.RegisterRequest
import com.kokodi.exception.AuthException
import com.kokodi.repository.UserRepository
import com.kokodi.security.JwtService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager,
    private val userDetailsService: UserDetailsService
) {
    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByUsername(request.username)) {
            throw AuthException("Username already exists")
        }

        val user = User(
            username = request.username,
            password = passwordEncoder.encode(request.password),
            name = request.name
        )
        userRepository.save(user)

        val userDetails = userDetailsService.loadUserByUsername(user.username)
        val token = jwtService.generateToken(userDetails)

        return AuthResponse(token)
    }

    fun login(request: AuthRequest): AuthResponse {
        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.username, request.password)
            )

            val userDetails = userDetailsService.loadUserByUsername(request.username)
            val token = jwtService.generateToken(userDetails)

            return AuthResponse(token)
        } catch (e: Exception) {
            throw AuthException("Player not found")
        }
    }
} 