package com.touchgrass.models;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class GameCatalog {
    public static final String ENGINE_SNAKE = "snake";
    public static final String ENGINE_PONG = "pong";
    public static final String ENGINE_TIC_TAC_TOE = "tic-tac-toe";

    public static final List<GameDescriptor> GAMES = List.of(
            new GameDescriptor("snake", "Snake", "🐍", "Arcade survival", ENGINE_SNAKE, "#00FF87"),
            new GameDescriptor("snake-rush", "Snake Rush", "⚡", "Faster snake intensity", ENGINE_SNAKE, "#00FF87"),
            new GameDescriptor("neon-serpent", "Neon Serpent", "🧪", "Precision neon snake", ENGINE_SNAKE, "#00FF87"),
            new GameDescriptor("pong", "Pong", "🏓", "Classic paddle duel", ENGINE_PONG, "#00E5FF"),
            new GameDescriptor("pong-blitz", "Pong Blitz", "🚀", "High-tempo paddle action", ENGINE_PONG, "#00E5FF"),
            new GameDescriptor("air-hockey", "Air Hockey", "🛸", "Arcade puck showdown", ENGINE_PONG, "#00E5FF"),
            new GameDescriptor("tic-tac-toe", "Tic-Tac-Toe", "❌", "Strategic turn-based rounds", ENGINE_TIC_TAC_TOE, "#B44FFF"),
            new GameDescriptor("tic-tac-grid", "Tic-Tac Grid", "🧩", "Competitive board mindgame", ENGINE_TIC_TAC_TOE, "#B44FFF"),
            new GameDescriptor("neon-xo", "Neon XO", "💠", "Stylized X/O duels", ENGINE_TIC_TAC_TOE, "#B44FFF"));

    private static final Map<String, GameDescriptor> BY_ID =
            GAMES.stream().collect(Collectors.toUnmodifiableMap(GameDescriptor::gameId, descriptor -> descriptor));

    private GameCatalog() {
    }

    public static GameDescriptor getById(String gameId) {
        if (gameId == null) {
            return BY_ID.get(ENGINE_SNAKE);
        }
        return BY_ID.getOrDefault(gameId, BY_ID.get(ENGINE_SNAKE));
    }

    public static String resolveEngineId(String gameId) {
        return getById(gameId).engineId();
    }

    public static String resolveNeon(String gameId) {
        return getById(gameId).neonColor();
    }

    public record GameDescriptor(
            String gameId,
            String title,
            String icon,
            String subtitle,
            String engineId,
            String neonColor
    ) {
    }
}
