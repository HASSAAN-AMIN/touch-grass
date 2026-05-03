package com.touchgrass.bl;

import com.touchgrass.models.GameCatalog;

import java.util.UUID;

public final class GameFactory {
    public Session createSession(String gameId, String mode) {
        String selectedGameId = gameId == null ? GameCatalog.ENGINE_SNAKE : gameId;
        String sessionId = selectedGameId + "-" + UUID.randomUUID();
        return switch (mode) {
            case "SinglePlayer", "LocalCoOp" -> new LocalSession(sessionId, selectedGameId, mode, "WASD", "ArrowKeys");
            case "LAN" -> new NetworkSession(sessionId, selectedGameId, mode);
            default -> throw new IllegalArgumentException("Unsupported mode: " + mode);
        };
    }
}
