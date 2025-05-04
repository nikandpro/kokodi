package com.kokodi.domain

import jakarta.persistence.*

@Entity
@Table(name = "game_session_players")
data class GameSessionPlayer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "game_session_id")
    val gameSessionId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @Column(name = "join_order")
    val joinOrder: Int,

    @Column(name = "score")
    var score: Int = 0,

    @Column(name = "is_blocked")
    var isBlocked: Boolean = false
) 