# Touch Grass

A JavaFX desktop gaming hub built with a strict 3-tier architecture:

- `ui` - JavaFX views and rendering shell
- `bl` - business logic, controllers, sessions, game logic orchestration
- `db` - MySQL connection and persistence access
- `models` - core domain entities

## Tech Stack

- Java 17+
- JavaFX
- Maven
- MySQL Connector/J

## Run

```bash
mvn javafx:run
```

## Project Structure

```text
src/main/java/com/touchgrass/
  TouchGrassApp.java
  ui/
  bl/
    games/
  db/
  models/
```
