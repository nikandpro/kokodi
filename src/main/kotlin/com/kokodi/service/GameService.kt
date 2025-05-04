package com.kokodi.service

import com.kokodi.domain.*
import com.kokodi.dto.TurnRequest
import com.kokodi.exception.GameException
import com.kokodi.repository.CardRepository
import com.kokodi.repository.GameSessionRepository
import com.kokodi.repository.UserRepository
import github.nikandpro.com.kokodi.domain.ActionType
import org.apache.coyote.BadRequestException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GameService(
    private val gameSessionRepository: GameSessionRepository,
    private val userRepository: UserRepository,
    private val cardRepository: CardRepository,
    private val cardService: CardService
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
            .orElseThrow { BadRequestException("Game not found") }

        if (gameSession.status != GameStatus.WAITING_FOR_PLAYERS) {
            throw BadRequestException("Game is not waiting for players")
        }

        if (gameSession.players.size < 2) {
            throw BadRequestException("Not enough players")
        }

        val creator = gameSession.players.first()
        if (creator.user.username != username) {
            throw BadRequestException("Only game creator can start the game")
        }

        gameSession.status = GameStatus.IN_PROGRESS
        cardService.initializeDeck(gameSession)

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
            rotateToNextPlayer(gameSession)
            return Turn(
                gameSession = gameSession,
                player = currentPlayer,
                card = null,
                pointsChange = 0
            ).also { gameSession.turns.add(it) }
        }

        val topCard = gameSession.deck.firstOrNull { !it.isPlayed }
            ?: throw GameException("No cards left in deck")

        try {
            when (topCard.card) {
                is PointsCard -> cardService.handlePointsCard(gameSession, currentPlayer, topCard)
                is ActionCard -> cardService.handleActionCard(gameSession, currentPlayer, topCard, turnRequest)
                else -> throw GameException("Unknown card type")
            }
        } catch (e: Exception) {
            throw GameException("Error processing card: ${e.message}")
        }

        topCard.isPlayed = true

        if (currentPlayer.score >= 30) {
            gameSession.status = GameStatus.FINISHED
        } else {
            rotateToNextPlayer(gameSession)
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

    private fun rotateToNextPlayer(gameSession: GameSession) {
        gameSession.currentPlayerIndex = gameSession.nextPlayerIndex
        gameSession.nextPlayerIndex = (gameSession.nextPlayerIndex + 1) % gameSession.players.size
    }
} 