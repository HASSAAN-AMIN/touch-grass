package com.touchgrass.bl;

import com.touchgrass.bl.games.GameState;
import com.touchgrass.bl.games.InputCommand;
import com.touchgrass.bl.games.DodgerLogic;
import com.touchgrass.bl.games.MazeEscapeLogic;
import com.touchgrass.bl.games.PongLogic;
import com.touchgrass.bl.games.SnakeLogic;
import com.touchgrass.bl.games.TargetTapLogic;
import com.touchgrass.models.GameCatalog;
import com.touchgrass.models.TicTacToeLogic;

public final class LocalSession extends Session {
    private final String engineId;
    private SnakeLogic snakeLogic;
    private PongLogic pongLogic;
    private TicTacToeLogic ticTacToeLogic;
    private DodgerLogic dodgerLogic;
    private TargetTapLogic targetTapLogic;
    private MazeEscapeLogic mazeEscapeLogic;

    public LocalSession(String sessionId, String gameId, String mode, String p1Controls, String p2Controls) {
        super(sessionId, mode);
        this.engineId = GameCatalog.resolveEngineId(gameId);
        initializeGameLogic();
    }

    @Override
    public void start() {
    }

    @Override
    public void end() {
    }

    @Override
    public void handleInput(InputCommand inputCommand, boolean pressed) {
        if (snakeLogic != null && pressed) {
            snakeLogic.processCommand(inputCommand);
            return;
        }
        if (pongLogic != null && pressed) {
            pongLogic.processCommand(inputCommand, 1);
            return;
        }
        if (dodgerLogic != null && pressed) {
            if (inputCommand == InputCommand.LEFT) {
                dodgerLogic.moveLeft();
            } else if (inputCommand == InputCommand.RIGHT) {
                dodgerLogic.moveRight();
            }
            return;
        }
        if (mazeEscapeLogic != null && pressed) {
            switch (inputCommand) {
                case UP -> mazeEscapeLogic.moveUp();
                case DOWN -> mazeEscapeLogic.moveDown();
                case LEFT -> mazeEscapeLogic.moveLeft();
                case RIGHT -> mazeEscapeLogic.moveRight();
                default -> {
                }
            }
            return;
        }
        if (ticTacToeLogic != null && pressed && inputCommand == InputCommand.ACTION) {
            // ACTION is kept for keyboard-based move confirmation extensions.
        }
    }

    public void handleInputForPlayer(InputCommand inputCommand, int playerNumber, boolean pressed) {
        if (pongLogic == null || !pressed) {
            return;
        }
        pongLogic.processCommand(inputCommand, playerNumber);
    }

    @Override
    public void tick() {
        if (snakeLogic != null) {
            snakeLogic.update();
            return;
        }
        if (pongLogic != null) {
            if ("SinglePlayer".equalsIgnoreCase(getMode())) {
                driveSinglePlayerOpponent();
            }
            pongLogic.update();
            return;
        }
        if (dodgerLogic != null) {
            dodgerLogic.update();
            return;
        }
        if (targetTapLogic != null) {
            targetTapLogic.update();
        }
    }

    @Override
    public boolean isGameOver() {
        if (snakeLogic != null) {
            return snakeLogic.isGameOver();
        }
        if (dodgerLogic != null) {
            return dodgerLogic.isGameOver();
        }
        if (targetTapLogic != null) {
            return targetTapLogic.isGameOver();
        }
        if (mazeEscapeLogic != null) {
            return mazeEscapeLogic.isGameOver();
        }
        return ticTacToeLogic != null && ticTacToeLogic.isGameOver();
    }

    @Override
    public int getScore() {
        if (snakeLogic != null) {
            return snakeLogic.getScore();
        }
        if (dodgerLogic != null) {
            return dodgerLogic.getScore();
        }
        if (targetTapLogic != null) {
            return targetTapLogic.getScore();
        }
        if (mazeEscapeLogic != null) {
            return mazeEscapeLogic.getScore();
        }
        return 0;
    }

    @Override
    public GameState getCurrentGameState() {
        if (pongLogic == null) {
            return null;
        }
        return pongLogic.toGameState();
    }

    public SnakeLogic getSnakeLogic() {
        return snakeLogic;
    }

    public TicTacToeLogic getTicTacToeLogic() {
        return ticTacToeLogic;
    }

    public DodgerLogic getDodgerLogic() {
        return dodgerLogic;
    }

    public TargetTapLogic getTargetTapLogic() {
        return targetTapLogic;
    }

    public MazeEscapeLogic getMazeEscapeLogic() {
        return mazeEscapeLogic;
    }

    public boolean placeTicTacToeMark(int row, int col) {
        if (ticTacToeLogic == null) {
            return false;
        }
        return ticTacToeLogic.play(row, col);
    }

    public boolean handleTargetTap(double xRatio, double yRatio) {
        if (targetTapLogic == null) {
            return false;
        }
        return targetTapLogic.tap(xRatio, yRatio);
    }

    private void initializeGameLogic() {
        if (GameCatalog.ENGINE_SNAKE.equalsIgnoreCase(engineId)) {
            snakeLogic = new SnakeLogic();
            return;
        }
        if (GameCatalog.ENGINE_PONG.equalsIgnoreCase(engineId)) {
            pongLogic = new PongLogic();
            return;
        }
        if (GameCatalog.ENGINE_TIC_TAC_TOE.equalsIgnoreCase(engineId)) {
            ticTacToeLogic = new TicTacToeLogic();
            return;
        }
        if (GameCatalog.ENGINE_DODGER.equalsIgnoreCase(engineId)) {
            dodgerLogic = new DodgerLogic();
            return;
        }
        if (GameCatalog.ENGINE_TARGET_TAP.equalsIgnoreCase(engineId)) {
            targetTapLogic = new TargetTapLogic();
            return;
        }
        if (GameCatalog.ENGINE_MAZE_ESCAPE.equalsIgnoreCase(engineId)) {
            mazeEscapeLogic = new MazeEscapeLogic();
        }
    }

    private void driveSinglePlayerOpponent() {
        double ballCenter = pongLogic.getBallCenterY();
        double paddle2Center = pongLogic.getPaddleCenterY(2);
        if (Math.abs(ballCenter - paddle2Center) < 8) {
            return;
        }
        pongLogic.processCommand(ballCenter < paddle2Center ? InputCommand.UP : InputCommand.DOWN, 2);
    }
}
