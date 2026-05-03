package com.touchgrass.bl;

import com.touchgrass.bl.games.GameState;
import com.touchgrass.bl.games.InputCommand;
import com.touchgrass.bl.games.PongLogic;
import com.touchgrass.bl.games.SnakeLogic;
import com.touchgrass.models.TicTacToeLogic;

public final class LocalSession extends Session {
    private final String gameId;
    private SnakeLogic snakeLogic;
    private PongLogic pongLogic;
    private TicTacToeLogic ticTacToeLogic;

    public LocalSession(String sessionId, String gameId, String mode, String p1Controls, String p2Controls) {
        super(sessionId, mode);
        this.gameId = gameId;
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
            pongLogic.update();
        }
    }

    @Override
    public boolean isGameOver() {
        if (snakeLogic != null) {
            return snakeLogic.isGameOver();
        }
        return ticTacToeLogic != null && ticTacToeLogic.isGameOver();
    }

    @Override
    public int getScore() {
        return snakeLogic == null ? 0 : snakeLogic.getScore();
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

    public boolean placeTicTacToeMark(int row, int col) {
        if (ticTacToeLogic == null) {
            return false;
        }
        return ticTacToeLogic.play(row, col);
    }

    private void initializeGameLogic() {
        if ("snake".equalsIgnoreCase(gameId)) {
            snakeLogic = new SnakeLogic();
            return;
        }
        if ("pong".equalsIgnoreCase(gameId)) {
            pongLogic = new PongLogic();
            return;
        }
        if ("tic-tac-toe".equalsIgnoreCase(gameId)) {
            ticTacToeLogic = new TicTacToeLogic();
        }
    }
}
