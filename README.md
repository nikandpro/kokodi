# Kokodi Card Game

A Spring Boot application implementing a card game where players collect points by playing special cards.

## Technologies Used

- Kotlin
- Spring Boot
- PostgreSQL
- Docker
- JWT Authentication

## Prerequisites

- JDK 17 or higher
- Docker and Docker Compose
- Gradle

## Getting Started

1. Clone the repository
2. Start the PostgreSQL database:
   ```bash
   docker-compose up -d
   ```
3. Add jwt.secret in application.yml
For example: your-256-bit-secret-key-must-be-at-least-32-chars-long

The application will be available at `http://localhost:8080`

## API Endpoints

### Authentication

- `POST /api/auth/register` - Register a new user
  ```json
  {
    "username": "string",
    "password": "string",
    "name": "string"
  }
  ```

- `POST /api/auth/login` - Login
  ```json
  {
    "username": "string",
    "password": "string"
  }
  ```

### Game Management

- `POST /api/games` - Create a new game
- `POST /api/games/{gameId}/join` - Join an existing game
- `POST /api/games/{gameId}/start` - Start the game
- `POST /api/games/{gameId}/turn` - Make a turn
  ```json
  {
    "targetPlayerId": "long"
  }
  ```
- `GET /api/games/{gameId}` - Get game status

## Game Rules

1. Each game can have 2-4 players
2. Players take turns drawing and playing cards
3. Points cards add points to the player's score
4. Action cards have special effects:
   - Block: Makes the next player skip their turn
   - Steal: Takes points from another player
   - Double Down: Doubles the player's current score (max 30)
5. The game ends when a player reaches 30 points

