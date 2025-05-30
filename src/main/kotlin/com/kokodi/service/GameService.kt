package com.kokodi.service

import com.kokodi.domain.*
import com.kokodi.dto.TurnRequest
import com.kokodi.exception.GameException
import com.kokodi.repository.GameSessionRepository
import com.kokodi.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Value


@Service
class GameService(
    private val gameSessionRepository: GameSessionRepository,
    private val userRepository: UserRepository,
    private val cardService: CardService
) {
    @Value("\${game.max-players:4}")
    private var maxPlayers: Int = 4

    @Value("\${game.min-players:2}")
    private var minPlayers: Int = 2

    @Value("\${game.winning-score:30}")
    private var winningScore: Int = 30

    @Transactional
    fun createGame(username: String): GameSession {
        val user = userRepository.findByUsername(username)
            .orElseThrow { GameException("User not found") }

        val gameSession = GameSession()
        gameSession.currentPlayerIndex = 0
        gameSession.nextPlayerIndex = 1

        val savedGameSession = gameSessionRepository.save(gameSession)

        val player = GameSessionPlayer(
            gameSessionId = savedGameSession.id,
            user = user,
            joinOrder = 0
        )
        savedGameSession.players.add(player)

        return gameSessionRepository.save(savedGameSession)
    }

    @Transactional
    fun joinGame(gameId: Long, username: String): GameSession {
        val gameSession = gameSessionRepository.findById(gameId)
            .orElseThrow { GameException("Game not found") }

        validateGameCanJoin(gameSession, username)

        val user = userRepository.findByUsername(username)
            .orElseThrow { GameException("User not found") }

        val player = GameSessionPlayer(
            gameSessionId = gameSession.id,
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

        validateGameCanStart(gameSession, username)

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

        validateGameCanMakeTurn(gameSession, username)

        val currentPlayer = gameSession.players.getOrNull(gameSession.currentPlayerIndex)
            ?: throw GameException("Invalid current player index")

        if (currentPlayer.isBlocked) {
            currentPlayer.isBlocked = false
            rotateToNextPlayer(gameSession)
            return Turn(
                gameSessionId = gameSession.id,
                player = currentPlayer,
                card = null,
                pointsChange = 0
            ).also { gameSession.turns.add(it) }
        }

        val topCard = gameSession.deck.firstOrNull { !it.isUsed }
            ?: throw GameException("No cards left in deck")

        try {
            when (topCard) {
                is PointsCard -> cardService.handlePointsCard(gameSession, currentPlayer, topCard)
                is ActionCard -> cardService.handleActionCard(gameSession, currentPlayer, topCard, turnRequest)
                else -> throw GameException("Unknown card type")
            }
        } catch (e: Exception) {
            throw GameException("Error processing card: ${e.message}")
        }

        if (currentPlayer.score >= winningScore) {
            gameSession.status = GameStatus.FINISHED
        }

        rotateToNextPlayer(gameSession)
        return gameSessionRepository.save(gameSession).turns.last()
    }

    @Transactional
    fun getGameStatus(gameId: Long, username: String): GameSession {
        val gameSession = gameSessionRepository.findById(gameId)
            .orElseThrow { GameException("Game not found") }

        validatePlayerInGame(gameSession, username)

        return gameSession
    }

    private fun validateGameCanJoin(gameSession: GameSession, username: String) {
        if (gameSession.status != GameStatus.WAITING_FOR_PLAYERS) {
            throw GameException("Game is not waiting for players")
        }

        if (gameSession.players.size >= maxPlayers) {
            throw GameException("Game is full")
        }

        if (gameSession.players.any { it.user.username == username }) {
            throw GameException("User already in game")
        }
    }

    private fun validateGameCanStart(gameSession: GameSession, username: String) {
        if (gameSession.status != GameStatus.WAITING_FOR_PLAYERS) {
            throw GameException("Game is not waiting for players")
        }

        if (gameSession.players.size < minPlayers) {
            throw GameException("Not enough players")
        }

        val creator = gameSession.players.first()
        if (creator.user.username != username) {
            throw GameException("Only game creator can start the game")
        }
    }

    private fun validateGameCanMakeTurn(gameSession: GameSession, username: String) {
        if (gameSession.status != GameStatus.IN_PROGRESS) {
            throw GameException("Game is not in progress")
        }

        val currentPlayer = gameSession.players.getOrNull(gameSession.currentPlayerIndex)
            ?: throw GameException("Invalid current player index")

        if (currentPlayer.user.username != username) {
            throw GameException("Not your turn. Current player is ${currentPlayer.user.username}")
        }
    }

    private fun validatePlayerInGame(gameSession: GameSession, username: String) {
        if (!gameSession.players.any { it.user.username == username }) {
            throw GameException("You are not a player in this game")
        }
    }

    private fun rotateToNextPlayer(gameSession: GameSession): GameSession {
        gameSession.currentPlayerIndex = gameSession.nextPlayerIndex
        gameSession.nextPlayerIndex = (gameSession.nextPlayerIndex + 1) % gameSession.players.size
        return gameSession
    }
} 