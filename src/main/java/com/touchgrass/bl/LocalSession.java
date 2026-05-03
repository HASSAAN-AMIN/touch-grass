package com.touchgrass.bl;

import com.touchgrass.bl.games.CarRaceLogic;
import com.touchgrass.bl.games.DodgerLogic;
import com.touchgrass.bl.games.DriftTrackLogic;
import com.touchgrass.bl.games.DriftTrackState;
import com.touchgrass.bl.games.GameState;
import com.touchgrass.bl.games.InputCommand;
import com.touchgrass.bl.games.MazeEscapeLogic;
import com.touchgrass.bl.games.PongLogic;
import com.touchgrass.bl.games.ShooterLogic;
import com.touchgrass.bl.games.SnakeLogic;
import com.touchgrass.bl.games.SudokuLogic;
import com.touchgrass.models.GameCatalog;
import com.touchgrass.models.TicTacToeLogic;

public final class LocalSession extends Session {
    private final String engineId;
    private SnakeLogic snakeLogic;
    private PongLogic pongLogic;
    private TicTacToeLogic ticTacToeLogic;
    private DodgerLogic dodgerLogic;
    private MazeEscapeLogic mazeEscapeLogic;
    private CarRaceLogic carRaceLogic;
    private SudokuLogic sudokuLogic;
    private ShooterLogic shooterLogic;
    private DriftTrackLogic driftTrackLogic;

    public LocalSession(String sessionId, String gameId, String mode) {
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
        if (carRaceLogic != null && pressed) {
            if (inputCommand == InputCommand.LEFT) {
                carRaceLogic.steerLeft();
            } else if (inputCommand == InputCommand.RIGHT) {
                carRaceLogic.steerRight();
            }
            return;
        }
        if (shooterLogic != null) {
            if (inputCommand == InputCommand.LEFT) {
                shooterLogic.setMovingLeft(pressed);
            } else if (inputCommand == InputCommand.RIGHT) {
                shooterLogic.setMovingRight(pressed);
            } else if (inputCommand == InputCommand.ACTION && pressed) {
                shooterLogic.fire();
            }
            return;
        }
        if (driftTrackLogic != null && pressed) {
            if (inputCommand == InputCommand.LEFT) {
                driftTrackLogic.steerPlayerLeft(1);
            } else if (inputCommand == InputCommand.RIGHT) {
                driftTrackLogic.steerPlayerRight(1);
            }
        }
    }

    public void handleInputForPlayer(InputCommand inputCommand, int playerNumber, boolean pressed) {
        if (pongLogic != null && pressed) {
            pongLogic.processCommand(inputCommand, playerNumber);
            return;
        }
        if (driftTrackLogic != null && pressed) {
            if (inputCommand == InputCommand.LEFT) {
                driftTrackLogic.steerPlayerLeft(playerNumber);
            } else if (inputCommand == InputCommand.RIGHT) {
                driftTrackLogic.steerPlayerRight(playerNumber);
            }
        }
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
        if (carRaceLogic != null) {
            carRaceLogic.update();
            return;
        }
        if (sudokuLogic != null) {
            sudokuLogic.update();
            return;
        }
        if (shooterLogic != null) {
            shooterLogic.update();
            return;
        }
        if (driftTrackLogic != null) {
            driftTrackLogic.update();
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
        if (mazeEscapeLogic != null) {
            return mazeEscapeLogic.isGameOver();
        }
        if (carRaceLogic != null) {
            return carRaceLogic.isGameOver();
        }
        if (sudokuLogic != null) {
            return sudokuLogic.isGameOver();
        }
        if (shooterLogic != null) {
            return shooterLogic.isGameOver();
        }
        if (driftTrackLogic != null) {
            return driftTrackLogic.isGameOver();
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
        if (mazeEscapeLogic != null) {
            return mazeEscapeLogic.getScore();
        }
        if (carRaceLogic != null) {
            return carRaceLogic.getScore();
        }
        if (sudokuLogic != null) {
            return sudokuLogic.getScore();
        }
        if (shooterLogic != null) {
            return shooterLogic.getScore();
        }
        if (driftTrackLogic != null) {
            return driftTrackLogic.getCombinedScore();
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

    @Override
    public DriftTrackState getDriftTrackState() {
        if (driftTrackLogic == null) {
            return null;
        }
        return driftTrackLogic.toState();
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

    public MazeEscapeLogic getMazeEscapeLogic() {
        return mazeEscapeLogic;
    }

    public CarRaceLogic getCarRaceLogic() {
        return carRaceLogic;
    }

    public SudokuLogic getSudokuLogic() {
        return sudokuLogic;
    }

    public ShooterLogic getShooterLogic() {
        return shooterLogic;
    }

    public DriftTrackLogic getDriftTrackLogic() {
        return driftTrackLogic;
    }

    public boolean placeTicTacToeMark(int row, int col) {
        if (ticTacToeLogic == null) {
            return false;
        }
        return ticTacToeLogic.play(row, col);
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
        if (GameCatalog.ENGINE_MAZE_ESCAPE.equalsIgnoreCase(engineId)) {
            mazeEscapeLogic = new MazeEscapeLogic();
            return;
        }
        if (GameCatalog.ENGINE_CAR_RACE.equalsIgnoreCase(engineId)) {
            carRaceLogic = new CarRaceLogic();
            return;
        }
        if (GameCatalog.ENGINE_SUDOKU.equalsIgnoreCase(engineId)) {
            sudokuLogic = new SudokuLogic();
            return;
        }
        if (GameCatalog.ENGINE_SHOOTER.equalsIgnoreCase(engineId)) {
            shooterLogic = new ShooterLogic();
            return;
        }
        if (GameCatalog.ENGINE_DRIFT_TRACK.equalsIgnoreCase(engineId)) {
            driftTrackLogic = new DriftTrackLogic();
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
