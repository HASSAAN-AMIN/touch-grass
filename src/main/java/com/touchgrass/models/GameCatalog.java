package com.touchgrass.models;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class GameCatalog {
    public static final String ENGINE_SNAKE = "snake";
    public static final String ENGINE_PONG = "pong";
    public static final String ENGINE_TIC_TAC_TOE = "tic-tac-toe";
    public static final String ENGINE_DODGER = "dodger";
    public static final String ENGINE_MAZE_ESCAPE = "maze-escape";
    public static final String ENGINE_CAR_RACE = "car-race";
    public static final String ENGINE_SUDOKU = "sudoku";
    public static final String ENGINE_SHOOTER = "shooter";
    public static final String ENGINE_DRIFT_TRACK = "drift-track";

    public static final List<GameDescriptor> GAMES = List.of(
            new GameDescriptor("snake", "Snake", "🐍", "Arcade survival classic", ENGINE_SNAKE, "#00FF87", true, false, false),
            new GameDescriptor("pong", "Pong", "🏓", "Classic paddle duel", ENGINE_PONG, "#00E5FF", true, true, true),
            new GameDescriptor("tic-tac-toe", "Tic-Tac-Toe", "❌", "Strategic turn-based rounds", ENGINE_TIC_TAC_TOE, "#B44FFF", true, false, false),
            new GameDescriptor("dodger", "Dodger", "🛡️", "Avoid falling blocks", ENGINE_DODGER, "#FF8C42", true, false, false),
            new GameDescriptor("maze-escape", "Maze Escape", "🧭", "Find your way to the exit", ENGINE_MAZE_ESCAPE, "#B44FFF", true, false, false),
            new GameDescriptor("car-race", "Neon Racer", "🏎️", "Dodge traffic on the highway", ENGINE_CAR_RACE, "#FF8C42", true, false, false),
            new GameDescriptor("sudoku", "Sudoku", "🔢", "Full 9x9 number puzzle", ENGINE_SUDOKU, "#00E5FF", true, false, false),
            new GameDescriptor("shooter", "Sky Shooter", "🎯", "Take down flying invaders", ENGINE_SHOOTER, "#FF4D8F", true, false, false),
            new GameDescriptor("drift-track", "Drift Track", "🏁", "Two-car drift race over LAN", ENGINE_DRIFT_TRACK, "#00FF87", false, true, true));

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
            String neonColor,
            boolean supportsSinglePlayer,
            boolean supportsLocalCoOp,
            boolean supportsLan
    ) {
    }
}
