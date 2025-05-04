package com.kokodi.repository

import com.kokodi.domain.GameSessionPlayer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GameSessionPlayerRepository : JpaRepository<GameSessionPlayer, Long> {
    fun findByGameSessionId(gameSessionId: Long): List<GameSessionPlayer>
} 