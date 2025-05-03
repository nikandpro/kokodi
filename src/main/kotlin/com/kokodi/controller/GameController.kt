package com.kokodi.controller

import com.kokodi.dto.GameSessionResponse
import com.kokodi.dto.TurnRequest
import com.kokodi.dto.TurnResponse
import com.kokodi.exception.ErrorResponse
import com.kokodi.exception.GameException
import com.kokodi.service.GameService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/games")
class GameController(
    private val gameService: GameService
) {
    @PostMapping
    fun createGame(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<GameSessionResponse> {
        return try {
            val gameSession = gameService.createGame(userDetails.username)
            ResponseEntity.ok(GameSessionResponse.fromGameSession(gameSession))
        } catch (e: GameException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/{gameId}/join")
    fun joinGame(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<GameSessionResponse> {
        return try {
            val gameSession = gameService.joinGame(gameId, userDetails.username)
            ResponseEntity.ok(GameSessionResponse.fromGameSession(gameSession))
        } catch (e: GameException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/{gameId}/start")
    fun startGame(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<GameSessionResponse> {
        return try {
            val gameSession = gameService.startGame(gameId, userDetails.username)
            ResponseEntity.ok(GameSessionResponse.fromGameSession(gameSession))
        } catch (e: GameException) {
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }

    @PostMapping("/{gameId}/turn")
    fun makeTurn(
        @PathVariable gameId: Long,
        @RequestBody turnRequest: TurnRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Any> {
        return try {
            val turn = gameService.makeTurn(gameId, userDetails.username, turnRequest)
            ResponseEntity.ok(TurnResponse.fromTurn(turn))
        } catch (e: GameException) {
            ResponseEntity.badRequest().body(ErrorResponse(e.message ?: "Unknown error"))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(ErrorResponse("Internal server error: ${e.message}"))
        }
    }

    @GetMapping("/{gameId}")
    fun getGameStatus(
        @PathVariable gameId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<GameSessionResponse> {
        return try {
            val gameSession = gameService.getGameStatus(gameId, userDetails.username)
            ResponseEntity.ok(GameSessionResponse.fromGameSession(gameSession))
        } catch (e: GameException) {
            ResponseEntity.badRequest().build()
        }
    }
} 