package com.kokodi.domain

import jakarta.persistence.*

@Entity
@Table(name = "game_cards")
data class GameCard(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "game_session_id")
    val gameSession: GameSession,

    @ManyToOne
    @JoinColumn(name = "card_id")
    val card: Card,

    var position: Int,
    var isPlayed: Boolean = false
) 