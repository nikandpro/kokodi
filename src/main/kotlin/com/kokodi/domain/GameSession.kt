package com.kokodi.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "game_sessions")
data class GameSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    var status: GameStatus = GameStatus.WAITING_FOR_PLAYERS,

    @OneToMany(mappedBy = "gameSession", cascade = [CascadeType.ALL])
    val players: MutableList<GameSessionPlayer> = mutableListOf(),

    @OneToMany(mappedBy = "gameSession", cascade = [CascadeType.ALL])
    val deck: MutableList<GameCard> = mutableListOf(),

    @OneToMany(mappedBy = "gameSession", cascade = [CascadeType.ALL])
    val turns: MutableList<Turn> = mutableListOf(),

    var currentPlayerIndex: Int = 0,
    var nextPlayerIndex: Int = 0,
    var createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class GameStatus {
    WAITING_FOR_PLAYERS,
    IN_PROGRESS,
    FINISHED
} 