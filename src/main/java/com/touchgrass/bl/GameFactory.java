package com.touchgrass.bl;

import java.util.UUID;

public final class GameFactory {
    public Session createSession(String gameId, String mode) {
        String sessionId = gameId + "-" + UUID.randomUUID();
        return switch (mode) {
            case "SinglePlayer", "LocalCoOp" -> new LocalSession(sessionId, gameId, mode, "WASD", "ArrowKeys");
            case "LAN" -> new NetworkSession(sessionId, mode);
            default -> throw new IllegalArgumentException("Unsupported mode: " + mode);
        };
    }
}
