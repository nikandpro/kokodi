package com.kokodi.service

import com.kokodi.domain.*
import com.kokodi.dto.TurnRequest
import com.kokodi.exception.GameException
import com.kokodi.repository.CardRepository
import com.kokodi.repository.GameSessionRepository
import com.kokodi.repository.UserRepository
import github.nikandpro.com.kokodi.domain.ActionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GameService(
    private val gameSessionRepository: GameSessionRepository,
    private val userRepository: UserRepository,
    private val cardRepository: CardRepository
) {
    @Transactional
    fun createGame(username: String): GameSession {
        val user = userRepository.findByUsername(username)
            .orElseThrow { GameException("User not found") }

        val gameSession = GameSession()
        val player = GameSessionPlayer(
            gameSession = gameSession,
            user = user,
            joinOrder = 0
        )
        gameSession.players.add(player)
        gameSession.currentPlayerIndex = 0
        gameSession.nextPlayerIndex = 1

        return gameSessionRepository.save(gameSession)
    }

    @Transactional
    fun joinGame(gameId: Long, username: String): GameSession {
        val gameSession = gameSessionRepository.findById(gameId)
            .orElseThrow { GameException("Game not found") }

        if (gameSession.status != GameStatus.WAITING_FOR_PLAYERS) {
            throw GameException("Game is not waiting for players")
        }

        if (gameSession.players.size >= 4) {
            throw GameException("Game is full")
        }

        val user = userRepository.findByUsername(username)
            .orElseThrow { GameException("User not found") }

        if (gameSession.players.any { it.user.id == user.id }) {
            throw GameException("User already in game")
        }

        val player = GameSessionPlayer(
            gameSession = gameSession,
            user = user,
            joinOrder = gameSession.players.size
        )
        gameSession.players.add(player)

        if (gameSession.players.size > 1) {
            gameSession.nextPlayerIndex = 1
        }

        return gameSessionRepository.save(gameSession)
    }

    @Transactional
    fun startGame(gameId: Long, username: String): GameSession {
        val gameSession = gameSessionRepository.findById(gameId)
            .orElseThrow { GameException("Game not found") }

        if (gameSession.status != GameStatus.WAITING_FOR_PLAYERS) {
            throw GameException("Game is not waiting for players")
        }

        if (gameSession.players.size < 2) {
            throw GameException("Not enough players")
        }

        val creator = gameSession.players.first()
        if (creator.user.username != username) {
            throw GameException("Only game creator can start the game")
        }

        gameSession.status = GameStatus.IN_PROGRESS
        initializeDeck(gameSession)

        gameSession.currentPlayerIndex = 0
        gameSession.nextPlayerIndex = 1

        return gameSessionRepository.save(gameSession)
    }

    @Transactional
    fun makeTurn(gameId: Long, username: String, turnRequest: TurnRequest): Turn {
        val gameSession = gameSessionRepository.findById(gameId)
            .orElseThrow { GameException("Game not found") }

        if (gameSession.status != GameStatus.IN_PROGRESS) {
            throw GameException("Game is not in progress")
        }

        val currentPlayer = gameSession.players.getOrNull(gameSession.currentPlayerIndex)
            ?: throw GameException("Invalid current player index")

        if (currentPlayer.user.username != username) {
            throw GameException("Not your turn. Current player is ${currentPlayer.user.username}")
        }

        if (currentPlayer.isBlocked) {
            currentPlayer.isBlocked = false
            return Turn(
                gameSession = gameSession,
                player = currentPlayer,
                card = null,
                pointsChange = 0
            ).also { gameSession.turns.add(it) }
        }

        val topCard = gameSession.deck.firstOrNull { !it.isPlayed }
            ?: throw GameException("No cards left in deck")

        val turn = try {
            when (topCard.card) {
                is PointsCard -> handlePointsCard(gameSession, currentPlayer, topCard)
                is ActionCard -> handleActionCard(gameSession, currentPlayer, topCard, turnRequest)
                else -> throw GameException("Unknown card type")
            }
        } catch (e: Exception) {
            throw GameException("Error processing card: ${e.message}")
        }

        topCard.isPlayed = true

        if (currentPlayer.score >= 30) {
            gameSession.status = GameStatus.FINISHED
        }

        return gameSessionRepository.save(gameSession).turns.last()
    }

    @Transactional
    fun getGameStatus(gameId: Long, username: String): GameSession {
        val gameSession = gameSessionRepository.findById(gameId)
            .orElseThrow { GameException("Game not found") }

        if (!gameSession.players.any { it.user.username == username }) {
            throw GameException("You are not a player in this game")
        }

        return gameSession
    }

    private fun initializeDeck(gameSession: GameSession) {
        val cards = mutableListOf<Card>()

        repeat(5) {
            val card = PointsCard("Small Points", 2)
            cardRepository.save(card)
            cards.add(card)
        }
        repeat(3) {
            val card = PointsCard("Medium Points", 5)
            cardRepository.save(card)
            cards.add(card)
        }
        repeat(2) {
            val card = PointsCard("Large Points", 8)
            cardRepository.save(card)
            cards.add(card)
        }

        repeat(3) {
            val card = ActionCard("Block", 1, ActionType.BLOCK)
            cardRepository.save(card)
            cards.add(card)
        }
        repeat(2) {
            val card = ActionCard("Steal", 3, ActionType.STEAL)
            cardRepository.save(card)
            cards.add(card)
        }
        repeat(2) {
            val card = ActionCard("Double Down", 2, ActionType.DOUBLE_DOWN)
            cardRepository.save(card)
            cards.add(card)
        }

        cards.shuffle()

        cards.forEachIndexed { index, card ->
            gameSession.deck.add(
                GameCard(
                    gameSession = gameSession,
                    card = card,
                    position = index
                )
            )
        }
    }

    private fun handlePointsCard(
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

    private fun handleActionCard(
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