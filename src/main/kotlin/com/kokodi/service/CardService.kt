package com.kokodi.service

import com.kokodi.domain.*
import com.kokodi.dto.TurnRequest
import com.kokodi.exception.GameException
import com.kokodi.repository.CardRepository
import github.nikandpro.com.kokodi.domain.ActionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CardService(
    private val cardRepository: CardRepository
) {
    fun initializeDeck(gameSession: GameSession) {
        val cards = mutableListOf<Card>()


        repeat(5) {
            cards.add(PointsCard("Small Points", 2))
        }
        repeat(3) {
            cards.add(PointsCard("Medium Points", 5))
            cards.add(ActionCard("Block", 1, ActionType.BLOCK))
        }
        repeat(2) {
            cards.add(ActionCard("Steal", 3, ActionType.STEAL))
            cards.add(ActionCard("Double Down", 2, ActionType.DOUBLE_DOWN))
            cards.add(PointsCard("Large Points", 8))
        }


        cards.shuffle()

        cards.forEachIndexed { index, card ->
            val savedCard = cardRepository.save(card)
            gameSession.deck.add(
                GameCard(
                    gameSession = gameSession,
                    card = savedCard,
                    position = index
                )
            )
        }
    }

    fun handlePointsCard(
        gameSession: GameSession,
        player: GameSessionPlayer,
        gameCard: GameCard
    ): Turn {
        val pointsCard = gameCard.card as PointsCard
        player.score += pointsCard.value

        return Turn(
            gameSession = gameSession,
            player = player,
            card = pointsCard,
            pointsChange = pointsCard.value
        ).also { gameSession.turns.add(it) }
    }

    fun handleActionCard(
        gameSession: GameSession,
        player: GameSessionPlayer,
        gameCard: GameCard,
        turnRequest: TurnRequest
    ): Turn {
        val actionCard = gameCard.card as ActionCard
        var pointsChange = 0

        when (actionCard.actionType) {
            ActionType.BLOCK -> {
                val nextPlayer = gameSession.players[gameSession.nextPlayerIndex]
                nextPlayer.isBlocked = true
            }

            ActionType.STEAL -> {
                if (turnRequest.targetPlayerId == null) {
                    throw GameException("Target player ID is required for STEAL action")
                }
                val targetPlayer = gameSession.players.find { it.id == turnRequest.targetPlayerId }
                    ?: throw GameException("Target player not found")

                val stealAmount = minOf(actionCard.value, targetPlayer.score)
                targetPlayer.score -= stealAmount
                player.score += stealAmount
                pointsChange = stealAmount
            }

            ActionType.DOUBLE_DOWN -> {
                val newScore = player.score * 2
                pointsChange = newScore - player.score
                player.score = minOf(newScore, 30)
            }
        }

        return Turn(
            gameSession = gameSession,
            player = player,
            card = actionCard,
            targetPlayer = gameSession.players.find { it.id == turnRequest.targetPlayerId },
            pointsChange = pointsChange
        ).also { gameSession.turns.add(it) }
    }
} 