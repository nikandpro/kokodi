package com.kokodi.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "turns")
data class Turn(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "game_session_id")
    val gameSessionId: Long,

    @ManyToOne
    @JoinColumn(name = "player_id")
    val player: GameSessionPlayer,

    @ManyToOne
    @JoinColumn(name = "card_id")
    val card: Card? = null,

    @ManyToOne
    @JoinColumn(name = "target_player_id")
    val targetPlayer: GameSessionPlayer? = null,

    var pointsChange: Int = 0,
    var createdAt: LocalDateTime = LocalDateTime.now()
) 