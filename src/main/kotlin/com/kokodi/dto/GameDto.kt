package com.kokodi.dto

import com.kokodi.domain.*

data class GameSessionResponse(
    val id: Long,
    val status: GameStatus,
    val players: List<PlayerResponse>,
    val currentPlayer: PlayerResponse?,
    val nextPlayer: PlayerResponse?,
    val remainingCards: Int
) {
    companion object {
        fun fromGameSession(gameSession: GameSession): GameSessionResponse {
            return GameSessionResponse(
                id = gameSession.id,
                status = gameSession.status,
                players = gameSession.players.map { PlayerResponse.fromGameSessionPlayer(it) },
                currentPlayer = gameSession.players.getOrNull(gameSession.currentPlayerIndex)?.let {
                    PlayerResponse.fromGameSessionPlayer(it)
                },
                nextPlayer = gameSession.players.getOrNull(gameSession.nextPlayerIndex)?.let {
                    PlayerResponse.fromGameSessionPlayer(it)
                },
                remainingCards = gameSession.deck.count { !it.isPlayed }
            )
        }
    }
}

data class PlayerResponse(
    val id: Long,
    val name: String,
    val score: Int,
    val isBlocked: Boolean
) {
    companion object {
        fun fromGameSessionPlayer(player: GameSessionPlayer): PlayerResponse {
            return PlayerResponse(
                id = player.id,
                name = player.user.name,
                score = player.score,
                isBlocked = player.isBlocked
            )
        }
    }
}

data class TurnResponse(
    val id: Long,
    val player: PlayerResponse,
    val card: CardResponse?,
    val targetPlayer: PlayerResponse?,
    val pointsChange: Int
) {
    companion object {
        fun fromTurn(turn: Turn): TurnResponse {
            return TurnResponse(
                id = turn.id,
                player = PlayerResponse.fromGameSessionPlayer(turn.player),
                card = turn.card?.let { CardResponse.fromCard(it) },
                targetPlayer = turn.targetPlayer?.let { PlayerResponse.fromGameSessionPlayer(it) },
                pointsChange = turn.pointsChange
            )
        }
    }
}

data class CardResponse(
    val id: Long,
    val value: Int,
    val type: String,
    val actionType: String?
) {
    companion object {
        fun fromCard(card: Card): CardResponse {
            return when (card) {
                is PointsCard -> CardResponse(
                    id = card.id,
                    value = card.value,
                    type = "POINTS",
                    actionType = null
                )

                is ActionCard -> CardResponse(
                    id = card.id,
                    value = card.value,
                    type = "ACTION",
                    actionType = card.actionType.name
                )

                else -> throw IllegalArgumentException("Unknown card type")
            }
        }
    }
} 