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
            val card = PointsCard("Small Points", 2)
            card.gameSessionId = gameSession.id
            cards.add(card)
        }
        repeat(3) {
            val card = PointsCard("Medium Points", 5)
            card.gameSessionId = gameSession.id
            cards.add(card)
        }
        repeat(2) {
            val card = PointsCard("Large Points", 8)
            card.gameSessionId = gameSession.id
            cards.add(card)
        }

        repeat(3) {
            val card = ActionCard("Block", 1, ActionType.BLOCK)
            card.gameSessionId = gameSession.id
            cards.add(card)
        }
        repeat(2) {
            val card = ActionCard("Steal", 3, ActionType.STEAL)
            card.gameSessionId = gameSession.id
            cards.add(card)
        }
        repeat(2) {
            val card = ActionCard("Double Down", 2, ActionType.DOUBLE_DOWN)
            card.gameSessionId = gameSession.id
            cards.add(card)
        }

        cards.shuffle()

        cards.forEach { card ->
            val savedCard = cardRepository.save(card)

            gameSession.deck.add(savedCard)
        }
    }

    fun handlePointsCard(
        gameSession: GameSession,
        player: GameSessionPlayer,
        card: Card
    ): Turn {
        val pointsCard = card as PointsCard
        player.score += pointsCard.value

        return Turn(
            gameSessionId = gameSession.id,
            player = player,
            card = pointsCard,
            pointsChange = pointsCard.value
        ).also { gameSession.turns.add(it) }
    }

    fun handleActionCard(
        gameSession: GameSession,
        player: GameSessionPlayer,
        card: Card,
        turnRequest: TurnRequest
    ): Turn {
        val actionCard = card as ActionCard
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

        cardRepository.markCardAsUsed(card.id)

        return Turn(
            gameSessionId = gameSession.id,
            player = player,
            card = actionCard,
            targetPlayer = gameSession.players.find { it.id == turnRequest.targetPlayerId },
            pointsChange = pointsChange
        ).also { gameSession.turns.add(it) }
    }
}