# VIBE CODED PROJECT
` Not Really worth checking this shi is Vibe Coded uhh i m SORRY 😞`

# Touch Grass

Touch Grass is a JavaFX desktop gaming hub focused on clean architecture, responsive UI, and modular gameplay sessions.  
The project follows a strict 3-tier design:

- `ui`: JavaFX presentation layer and rendering
- `bl`: business logic, session orchestration, controllers, and game rules
- `db`: persistence and database connectivity
- `models`: domain entities and pure game models

## Core Features

- Account login and inline registration flow
- Default admin login (`admin` / `admin`) with auto-bootstrap profile creation
- Game lobby with modern UI and inline navigation
- Built-in settings for:
  - theme mode
  - accent style
  - FPS visibility
  - ambient motion visuals
- Playable modules:
  - Snake (score, dynamic speed progression, game-over flow)
  - Pong (single-player AI, local co-op, LAN multiplayer)
  - Tic-Tac-Toe (turn tracking, win/draw detection)
- Leaderboard persistence and top-score display
- LAN host/join flow for Pong using sockets (port `8080`)

## Architecture

### UI Layer (`com.touchgrass.ui`)

- Pure JavaFX views (`LoginView`, `MainLobbyView`, `GameView`)
- No SQL or direct DB access
- Scene transitions and input capture
- Canvas-based rendering loop for active games

### Business Logic Layer (`com.touchgrass.bl`)

- `SystemController` as central application controller
- Session polymorphism via `Session`, `LocalSession`, `NetworkSession`
- `GameFactory` for session creation
- `AccountManager` and `LeaderboardManager` for auth/score operations
- UI settings state managed in `UiSettings`

### Database Layer (`com.touchgrass.db`)

- `DatabaseConnection` singleton for shared JDBC connection lifecycle
- SQL exception handling delegated to BL managers

## Game Systems

### Snake

- Grid-based movement and collision handling
- Food spawning, growth, and score increments
- Dynamic tick speed scaling with score progression

### Pong

- Paddle and ball simulation with bounce physics
- Angle-influenced rebounds and progressive ball acceleration
- Single-player AI paddle in local mode
- Local co-op split-keyboard controls
- Host-authoritative LAN synchronization

### Tic-Tac-Toe

- 3x3 board logic in pure model class
- Turn-based move validation
- Win and draw evaluation

## Technology Stack

- Java 17+
- JavaFX
- Maven
- MySQL Connector/J
- JDBC

## Project Structure

```text
src/main/java/com/touchgrass/
  TouchGrassApp.java
  ui/
    LoginView.java
    MainLobbyView.java
    GameView.java
  bl/
    SystemController.java
    GameFactory.java
    Session.java
    LocalSession.java
    NetworkSession.java
    AccountManager.java
    LeaderboardManager.java
    UiSettings.java
    games/
      InputCommand.java
      SnakeLogic.java
      PongLogic.java
      GameState.java
  db/
    DatabaseConnection.java
  models/
    Account.java
    PlayerProfile.java
    Game.java
    TicTacToeLogic.java
schema.sql
```

## Setup

### 1) Prerequisites

- JDK 17 or newer
- Maven
- MySQL or MariaDB running locally

### 2) Create database and import schema

From project root:

```bash
mariadb -u root -e "CREATE DATABASE IF NOT EXISTS touchgrass;"
mariadb -u root touchgrass < schema.sql
```

### 3) Verify DB credentials

Current connection settings in `DatabaseConnection` are:

- URL: `jdbc:mysql://localhost:3306/touchgrass`
- USER: `root`
- PASSWORD: empty string

If your local database uses different credentials, update `DatabaseConnection.java` accordingly.

### 4) Build and run

```bash
mvn clean compile
mvn javafx:run
```

## Controls

### Global

- `ESC`: return to lobby
- `P`: pause/resume active local gameplay

### Snake

- `W/A/S/D` or arrow keys: movement direction

### Pong

- Single-player / host paddle: `W/S` or up/down
- Local co-op:
  - Player 1: `W/S`
  - Player 2: `UP/DOWN`

### Tic-Tac-Toe

- Mouse click on board cell to place mark

## LAN Multiplayer Flow (Pong)

- Host selects `LAN Multiplayer` -> `Host Game`
- Host shares displayed local IP
- Client selects `LAN Multiplayer` -> `Join Game`, enters host IP
- Connection establishes over port `8080`
- Host simulates authoritative game state and streams updates

## Error Handling Notes

- Database failures are handled with safe fallbacks and status messaging
- Network socket failures are captured and session resources are closed cleanly
- Game-over transitions are protected so UI navigation still returns to lobby

## Current Scope and Next Improvements

- Gameplay modules are implemented and playable
- Visual system supports multiple themes and accents
- Recommended next upgrades:
  - richer audio feedback
  - configurable key bindings
  - expanded leaderboard filters (per game/mode)
  - persistent local settings storage
