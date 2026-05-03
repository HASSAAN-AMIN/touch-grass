package com.touchgrass.bl;

import java.util.UUID;

public final class GameFactory {
    public Session createSession(String gameId, String mode) {
        String sessionId = gameId + "-" + UUID.randomUUID();
        return switch (mode) {
            case "SinglePlayer", "LocalCoOp" -> new LocalSession(sessionId, mode, "WASD", "ArrowKeys");
            case "LAN" -> new NetworkSession(sessionId, mode, "127.0.0.1", 7777);
            default -> throw new IllegalArgumentException("Unsupported mode: " + mode);
        };
    }
}
