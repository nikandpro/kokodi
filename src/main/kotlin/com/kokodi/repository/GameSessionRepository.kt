package com.kokodi.repository

import com.kokodi.domain.GameSession
import com.kokodi.domain.GameStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GameSessionRepository : JpaRepository<GameSession, Long> {
    fun findByStatus(status: GameStatus): List<GameSession>
    fun findByPlayersUserId(userId: Long): List<GameSession>
} 