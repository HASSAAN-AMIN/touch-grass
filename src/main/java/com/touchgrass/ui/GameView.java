package com.touchgrass.ui;

import com.touchgrass.bl.LocalSession;
import com.touchgrass.bl.Session;
import com.touchgrass.bl.SystemController;
import com.touchgrass.bl.UiSettings;
import com.touchgrass.bl.games.CarRaceLogic;
import com.touchgrass.bl.games.DriftTrackLogic;
import com.touchgrass.bl.games.DriftTrackState;
import com.touchgrass.bl.games.GameState;
import com.touchgrass.bl.games.InputCommand;
import com.touchgrass.bl.games.PongLogic;
import com.touchgrass.bl.games.ShooterLogic;
import com.touchgrass.bl.games.SnakeLogic;
import com.touchgrass.bl.games.SudokuLogic;
import com.touchgrass.models.GameCatalog;
import com.touchgrass.models.TicTacToeLogic;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GameView {
    private static final double SCENE_WIDTH = 960;
    private static final double SCENE_HEIGHT = 600;
    private static final long LOGIC_TICK_NS = 100_000_000L;
    private static final double MIN_CANVAS_WIDTH = 660;
    private static final double MIN_CANVAS_HEIGHT = 320;

    private final Stage stage;
    private final SystemController systemController;
    private final String gameId;
    private final Session activeSession;
    private final Canvas canvas;
    private final Set<KeyCode> pressedKeys;
    private final BorderPane root;
    private final StackPane playArea;
    private final VBox gameOverOverlay;
    private final Label gameOverScoreLabel;
    private final Label hudPrimaryLabel;
    private final Label hudScoreLabel;
    private final Label hudHintLabel;
    private final Label hudStatusLabel;
    private AnimationTimer animationTimer;
    private long lastFrameTime;
    private long lastLogicTickTime;
    private double fps;
    private boolean gameOverOverlayShown;
    private boolean gameOverActionTriggered;
    private boolean paused;
    private String inlineStatusMessage;

    public GameView(Stage stage, SystemController systemController, String gameId, Session activeSession) {
        this.stage = stage;
        this.systemController = systemController;
        this.gameId = gameId;
        this.activeSession = activeSession;
        this.canvas = new Canvas(900, 460);
        this.pressedKeys = ConcurrentHashMap.newKeySet();
        this.root = new BorderPane();
        this.playArea = new StackPane();
        this.gameOverOverlay = new VBox(12);
        this.gameOverScoreLabel = new Label();
        this.hudPrimaryLabel = new Label();
        this.hudScoreLabel = new Label();
        this.hudHintLabel = new Label();
        this.hudStatusLabel = new Label();
        this.inlineStatusMessage = "";
        this.paused = false;
    }

    public Parent createRoot() {
        boolean darkTheme = systemController.getUiSettings().getThemeMode() != UiSettings.ThemeMode.LIGHT;
        String neon = gameNeon();

        hudPrimaryLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-letter-spacing: 1.5px; -fx-text-fill: " + primaryText(darkTheme) + ";");
        hudPrimaryLabel.setEffect(new DropShadow(12, Color.web(neon, 0.50)));
        hudScoreLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: " + neon + ";");
        hudHintLabel.setStyle("-fx-font-size: 11.5px; -fx-text-fill: " + secondaryText(darkTheme) + ";");
        hudStatusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + danger(darkTheme) + ";");

        Button quitButton = new Button("QUIT");
        quitButton.setStyle(navButtonStyle(darkTheme));
        quitButton.setOnAction(event -> returnToLobby());

        VBox leftHud = new VBox(4, hudPrimaryLabel, hudScoreLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(12, leftHud, spacer, quitButton);
        topBar.setPadding(new Insets(14, 18, 8, 18));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: " + topBarBg(darkTheme) + "; -fx-border-color: transparent transparent rgba(0,229,255,0.10) transparent; -fx-border-width: 0 0 1 0;");

        canvas.setOnMouseClicked(this::handleCanvasClicked);
        VBox canvasContainer = new VBox(canvas);
        canvasContainer.setPadding(new Insets(8, 20, 8, 20));
        canvasContainer.setAlignment(Pos.CENTER);

        setupGameOverOverlay(darkTheme);
        playArea.getChildren().setAll(canvasContainer, gameOverOverlay);

        HBox bottomBar = new HBox(18, hudHintLabel, hudStatusLabel);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(2, 20, 14, 20));

        root.setTop(topBar);
        root.setCenter(playArea);
        root.setBottom(bottomBar);
        root.setStyle("-fx-background-color: " + appBackground(darkTheme) + ";");
        root.setFocusTraversable(true);

        playArea.widthProperty().addListener((obs, oldValue, newValue) -> resizeCanvas());
        playArea.heightProperty().addListener((obs, oldValue, newValue) -> resizeCanvas());
        Platform.runLater(this::resizeCanvas);

        updateHud();
        return root;
    }

    public Scene createScene() {
        Scene scene = new Scene(createRoot(), SCENE_WIDTH, SCENE_HEIGHT);
        bindToScene(scene);
        return scene;
    }

    public void bindToScene(Scene scene) {
        scene.setOnKeyPressed(this::handleKeyPressed);
        scene.setOnKeyReleased(this::handleKeyReleased);
        root.requestFocus();
    }

    public void startGameLoop() {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        lastFrameTime = 0L;
        lastLogicTickTime = 0L;
        gameOverOverlayShown = false;
        gameOverActionTriggered = false;
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrameTime > 0) {
                    long deltaNanos = now - lastFrameTime;
                    if (deltaNanos > 0) {
                        fps = 1_000_000_000.0 / deltaNanos;
                    }
                }
                lastFrameTime = now;

                if (shouldTickLogic(now)) {
                    pumpDualPlayerInputs();
                    if (!paused) {
                        activeSession.tick();
                    }
                    lastLogicTickTime = now;
                }
                if (!gameOverOverlayShown && activeSession.isGameOver() && !isTicTacToeGame()) {
                    stopGameLoop();
                    showGameOverOverlay();
                }
                renderFrame(graphics);
                updateHud();
            }
        };
        animationTimer.start();
    }

    public void stopGameLoop() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    private void resizeCanvas() {
        double width = Math.max(MIN_CANVAS_WIDTH, playArea.getWidth() - 42);
        double height = Math.max(MIN_CANVAS_HEIGHT, playArea.getHeight() - 18);
        canvas.setWidth(width);
        canvas.setHeight(height);
    }

    private void renderFrame(GraphicsContext graphics) {
        boolean darkTheme = systemController.getUiSettings().getThemeMode() != UiSettings.ThemeMode.LIGHT;
        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
        graphics.setFill(darkTheme ? Color.web("#070D1C") : Color.web("#EEF5FF"));
        graphics.fillRect(0, 0, canvasWidth, canvasHeight);
        drawDotGrid(graphics, darkTheme, canvasWidth, canvasHeight);
        drawAmbientEffects(graphics, darkTheme, canvasWidth, canvasHeight);

        drawPongIfActive(graphics, canvasWidth, canvasHeight, darkTheme);
        drawSnakeIfActive(graphics, canvasWidth, canvasHeight, darkTheme);
        drawTicTacToeIfActive(graphics, canvasWidth, canvasHeight, darkTheme);
        drawDodgerIfActive(graphics, canvasWidth, canvasHeight, darkTheme);
        drawMazeIfActive(graphics, canvasWidth, canvasHeight, darkTheme);
        drawCarRaceIfActive(graphics, canvasWidth, canvasHeight, darkTheme);
        drawSudokuIfActive(graphics, canvasWidth, canvasHeight, darkTheme);
        drawShooterIfActive(graphics, canvasWidth, canvasHeight, darkTheme);
        drawDriftTrackIfActive(graphics, canvasWidth, canvasHeight, darkTheme);
    }

    private void drawDotGrid(GraphicsContext graphics, boolean darkTheme, double width, double height) {
        double spacing = 26;
        double radius = 1.5;
        graphics.setFill(darkTheme ? Color.web("#0F1E38") : Color.web("#D1DEEF"));
        for (double y = 0; y < height; y += spacing) {
            for (double x = 0; x < width; x += spacing) {
                graphics.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            }
        }
    }

    private void updateHud() {
        UiSettings uiSettings = systemController.getUiSettings();
        hudPrimaryLabel.setText(gameName() + " - " + activeSession.getMode());

        String scoreText = buildScoreText();
        if (uiSettings.isShowFps()) {
            scoreText += " | " + Math.round(fps) + " FPS";
        }
        hudScoreLabel.setText(scoreText);
        hudHintLabel.setText(buildHintText());

        if (paused) {
            hudStatusLabel.setText("PAUSED");
        } else if (!inlineStatusMessage.isBlank()) {
            hudStatusLabel.setText(inlineStatusMessage);
        } else {
            hudStatusLabel.setText("");
        }
    }

    private String buildScoreText() {
        if (isSnakeGame() && activeSession instanceof LocalSession localSession && localSession.getSnakeLogic() != null) {
            return "SCORE " + localSession.getSnakeLogic().getScore() + " | LENGTH " + localSession.getSnakeLogic().getSnakeBody().size();
        }
        if (isPongGame()) {
            GameState state = activeSession.getCurrentGameState();
            return state == null ? "SYNCING..." : "P1 " + state.scorePlayer1() + "  :  P2 " + state.scorePlayer2();
        }
        if (isTicTacToeGame() && activeSession instanceof LocalSession localSession && localSession.getTicTacToeLogic() != null) {
            TicTacToeLogic logic = localSession.getTicTacToeLogic();
            return logic.isGameOver()
                    ? (logic.isDraw() ? "RESULT DRAW" : "WINNER " + logic.getWinner())
                    : "TURN " + logic.getCurrentPlayer();
        }
        if (isDodgerGame() && activeSession instanceof LocalSession localSession && localSession.getDodgerLogic() != null) {
            return "SCORE " + localSession.getDodgerLogic().getScore();
        }
        if (isMazeEscapeGame() && activeSession instanceof LocalSession localSession && localSession.getMazeEscapeLogic() != null) {
            return "STEPS " + localSession.getMazeEscapeLogic().getSteps() + " | SCORE " + localSession.getMazeEscapeLogic().getScore();
        }
        if (isCarRaceGame() && activeSession instanceof LocalSession localSession && localSession.getCarRaceLogic() != null) {
            CarRaceLogic logic = localSession.getCarRaceLogic();
            return "DIST " + logic.getDistance() + " | SPEED " + String.format("%.1f", logic.getScrollSpeed());
        }
        if (isSudokuGame() && activeSession instanceof LocalSession localSession && localSession.getSudokuLogic() != null) {
            SudokuLogic logic = localSession.getSudokuLogic();
            return "SCORE " + logic.getScore() + " | MISTAKES " + logic.getMistakes() + "/5 | HINTS " + logic.getHintsRemaining();
        }
        if (isShooterGame() && activeSession instanceof LocalSession localSession && localSession.getShooterLogic() != null) {
            ShooterLogic logic = localSession.getShooterLogic();
            return "SCORE " + logic.getScore() + " | LIVES " + logic.getLives();
        }
        if (isDriftTrackGame()) {
            DriftTrackState state = activeSession.getDriftTrackState();
            if (state == null) {
                return "SYNCING...";
            }
            return "P1 " + state.getPlayer1Distance() + "  :  P2 " + state.getPlayer2Distance();
        }
        return "SESSION ACTIVE";
    }

    private String buildHintText() {
        if (isSudokuGame()) {
            return "CLICK CELL | 1-9 PLACE | 0/BACKSPACE CLEAR | H HINT | ESC QUIT";
        }
        if (isShooterGame()) {
            return "A/D MOVE | SPACE FIRE | P PAUSE | ESC QUIT";
        }
        if (isCarRaceGame()) {
            return "A/D STEER | P PAUSE | ESC QUIT";
        }
        if (isDriftTrackGame()) {
            return "P1 A/D | P2 LEFT/RIGHT | ESC QUIT";
        }
        return "WASD + ARROWS | P PAUSE | ESC QUIT";
    }

    private void handleKeyPressed(KeyEvent event) {
        KeyCode keyCode = event.getCode();
        if (keyCode == KeyCode.ESCAPE) {
            returnToLobby();
            return;
        }
        if (keyCode == KeyCode.P) {
            paused = !paused;
            return;
        }
        if (isSudokuGame() && activeSession instanceof LocalSession localSession && localSession.getSudokuLogic() != null) {
            handleSudokuKey(localSession.getSudokuLogic(), keyCode);
            return;
        }
        if (isLocalCoOpPong() || isDriftTrackGame()) {
            if (isDualPlayerKey(keyCode)) {
                pressedKeys.add(keyCode);
            }
            return;
        }
        InputCommand command = toInputCommand(keyCode);
        if (command == null) {
            return;
        }
        if (pressedKeys.add(keyCode)) {
            activeSession.handleInput(command, true);
        } else if (isPongGame()) {
            activeSession.handleInput(command, true);
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        KeyCode keyCode = event.getCode();
        if (isLocalCoOpPong() || isDriftTrackGame()) {
            pressedKeys.remove(keyCode);
            return;
        }
        InputCommand command = toInputCommand(keyCode);
        if (command == null) {
            return;
        }
        if (pressedKeys.remove(keyCode)) {
            activeSession.handleInput(command, false);
        }
    }

    private void handleSudokuKey(SudokuLogic logic, KeyCode keyCode) {
        switch (keyCode) {
            case DIGIT1, NUMPAD1 -> logic.enterValue(1);
            case DIGIT2, NUMPAD2 -> logic.enterValue(2);
            case DIGIT3, NUMPAD3 -> logic.enterValue(3);
            case DIGIT4, NUMPAD4 -> logic.enterValue(4);
            case DIGIT5, NUMPAD5 -> logic.enterValue(5);
            case DIGIT6, NUMPAD6 -> logic.enterValue(6);
            case DIGIT7, NUMPAD7 -> logic.enterValue(7);
            case DIGIT8, NUMPAD8 -> logic.enterValue(8);
            case DIGIT9, NUMPAD9 -> logic.enterValue(9);
            case DIGIT0, NUMPAD0, BACK_SPACE, DELETE -> logic.enterValue(0);
            case H -> {
                if (!logic.useHint()) {
                    inlineStatusMessage = "Hint unavailable.";
                } else {
                    inlineStatusMessage = "Hint applied.";
                }
            }
            default -> {
            }
        }
    }

    private InputCommand toInputCommand(KeyCode keyCode) {
        return switch (keyCode) {
            case W, UP -> InputCommand.UP;
            case S, DOWN -> InputCommand.DOWN;
            case A, LEFT -> InputCommand.LEFT;
            case D, RIGHT -> InputCommand.RIGHT;
            case SPACE, ENTER -> InputCommand.ACTION;
            default -> null;
        };
    }

    private void drawSnakeIfActive(GraphicsContext graphics, double canvasWidth, double canvasHeight, boolean darkTheme) {
        if (!isSnakeGame() || !(activeSession instanceof LocalSession localSession)) {
            return;
        }
        SnakeLogic snakeLogic = localSession.getSnakeLogic();
        if (snakeLogic == null) {
            return;
        }

        double boardSize = Math.min(canvasWidth * 0.78, canvasHeight * 0.88);
        double startX = (canvasWidth - boardSize) / 2.0;
        double startY = (canvasHeight - boardSize) / 2.0;
        double tileSize = boardSize / SnakeLogic.GRID_WIDTH;

        drawBoardFrame(graphics, startX, startY, boardSize, boardSize, gameNeon("snake"), darkTheme);

        graphics.setFill(Color.web(gameNeon("snake")));
        for (SnakeLogic.Cell cell : snakeLogic.getSnakeBody()) {
            double x = startX + (cell.x() * tileSize) + 2;
            double y = startY + (cell.y() * tileSize) + 2;
            double size = tileSize - 4;
            graphics.fillRoundRect(x, y, size, size, 8, 8);
        }

        SnakeLogic.Cell head = snakeLogic.getSnakeBody().isEmpty() ? null : snakeLogic.getSnakeBody().get(0);
        if (head != null) {
            double hx = startX + (head.x() * tileSize) + 1.6;
            double hy = startY + (head.y() * tileSize) + 1.6;
            double hSize = tileSize - 3.2;
            graphics.setFill(Color.web(gameNeon("snake"), 0.35));
            graphics.fillRoundRect(hx - 3, hy - 3, hSize + 6, hSize + 6, 10, 10);
            graphics.setFill(Color.web(gameNeon("snake")));
            graphics.fillRoundRect(hx, hy, hSize, hSize, 8, 8);
        }

        SnakeLogic.Cell food = snakeLogic.getFood();
        double foodX = startX + (food.x() * tileSize) + 2;
        double foodY = startY + (food.y() * tileSize) + 2;
        double foodSize = tileSize - 4;
        graphics.setFill(Color.web("#FF8C42"));
        graphics.fillRoundRect(foodX, foodY, foodSize, foodSize, 10, 10);
    }

    private void drawPongIfActive(GraphicsContext graphics, double canvasWidth, double canvasHeight, boolean darkTheme) {
        if (!isPongGame()) {
            return;
        }

        GameState state = activeSession.getCurrentGameState();
        if (state == null) {
            graphics.setFill(Color.web("#4A6A8A"));
            graphics.setFont(Font.font("Consolas", 20));
            graphics.fillText("WAITING FOR NETWORK STATE...", canvasWidth * 0.27, canvasHeight * 0.52);
            return;
        }

        double boardPadding = 18;
        double scale = Math.min((canvasWidth - (boardPadding * 2)) / PongLogic.FIELD_WIDTH,
                (canvasHeight - (boardPadding * 2)) / PongLogic.FIELD_HEIGHT);
        double drawWidth = PongLogic.FIELD_WIDTH * scale;
        double drawHeight = PongLogic.FIELD_HEIGHT * scale;
        double startX = (canvasWidth - drawWidth) / 2.0;
        double startY = (canvasHeight - drawHeight) / 2.0;

        drawBoardFrame(graphics, startX, startY, drawWidth, drawHeight, gameNeon("pong"), darkTheme);

        graphics.setStroke(Color.web(gameNeon("pong"), 0.60));
        graphics.setLineWidth(Math.max(2, 3 * scale));
        graphics.setLineDashes(12 * scale, 12 * scale);
        double centerX = startX + (drawWidth / 2.0);
        graphics.strokeLine(centerX, startY + (12 * scale), centerX, startY + drawHeight - (12 * scale));
        graphics.setLineDashes(0);

        graphics.setFill(Color.web(gameNeon("snake")));
        graphics.fillRoundRect(
                startX + (24 * scale),
                startY + (state.paddle1Y() * scale),
                PongLogic.PADDLE_WIDTH * scale,
                PongLogic.PADDLE_HEIGHT * scale,
                10 * scale,
                10 * scale);

        graphics.setFill(Color.web(gameNeon("tic-tac-toe")));
        graphics.fillRoundRect(
                startX + ((PongLogic.FIELD_WIDTH - 24 - PongLogic.PADDLE_WIDTH) * scale),
                startY + (state.paddle2Y() * scale),
                PongLogic.PADDLE_WIDTH * scale,
                PongLogic.PADDLE_HEIGHT * scale,
                10 * scale,
                10 * scale);

        graphics.setFill(Color.web("#FF8C42"));
        graphics.fillRoundRect(
                startX + (state.ballX() * scale),
                startY + (state.ballY() * scale),
                PongLogic.BALL_SIZE * scale,
                PongLogic.BALL_SIZE * scale,
                10 * scale,
                10 * scale);

        graphics.setFont(Font.font("Consolas", Math.max(18, 24 * scale)));
        graphics.setFill(Color.web("#E8F4FF"));
        graphics.fillText(String.valueOf(state.scorePlayer1()), centerX - (36 * scale), startY + (42 * scale));
        graphics.setFill(Color.web("#4A6A8A"));
        graphics.fillText(":", centerX - (6 * scale), startY + (42 * scale));
        graphics.setFill(Color.web("#E8F4FF"));
        graphics.fillText(String.valueOf(state.scorePlayer2()), centerX + (14 * scale), startY + (42 * scale));
    }

    private void drawTicTacToeIfActive(GraphicsContext graphics, double canvasWidth, double canvasHeight, boolean darkTheme) {
        if (!isTicTacToeGame() || !(activeSession instanceof LocalSession localSession)) {
            return;
        }
        TicTacToeLogic logic = localSession.getTicTacToeLogic();
        if (logic == null) {
            return;
        }

        double boardSize = Math.min(canvasWidth * 0.72, canvasHeight * 0.88);
        double startX = (canvasWidth - boardSize) / 2.0;
        double startY = (canvasHeight - boardSize) / 2.0;
        double cellSize = boardSize / TicTacToeLogic.GRID_SIZE;

        drawBoardFrame(graphics, startX, startY, boardSize, boardSize, gameNeon("tic-tac-toe"), darkTheme);

        graphics.setStroke(Color.web(gameNeon("tic-tac-toe"), 0.70));
        graphics.setLineWidth(Math.max(2, boardSize * 0.006));
        for (int i = 1; i < TicTacToeLogic.GRID_SIZE; i++) {
            double pos = startX + (i * cellSize);
            graphics.strokeLine(pos, startY, pos, startY + boardSize);
            pos = startY + (i * cellSize);
            graphics.strokeLine(startX, pos, startX + boardSize, pos);
        }

        char[][] board = logic.getBoardCopy();
        graphics.setFont(Font.font("Consolas", Math.max(38, boardSize * 0.17)));
        for (int row = 0; row < TicTacToeLogic.GRID_SIZE; row++) {
            for (int col = 0; col < TicTacToeLogic.GRID_SIZE; col++) {
                char value = board[row][col];
                if (value == ' ') {
                    continue;
                }
                graphics.setFill(value == 'X' ? Color.web(gameNeon("snake")) : Color.web(gameNeon("pong")));
                double textX = startX + (col * cellSize) + (cellSize * 0.34);
                double textY = startY + (row * cellSize) + (cellSize * 0.70);
                graphics.fillText(String.valueOf(value), textX, textY);
            }
        }
    }

    private void drawDodgerIfActive(GraphicsContext graphics, double canvasWidth, double canvasHeight, boolean darkTheme) {
        if (!isDodgerGame() || !(activeSession instanceof LocalSession localSession) || localSession.getDodgerLogic() == null) {
            return;
        }
        var logic = localSession.getDodgerLogic();
        double boardWidth = canvasWidth * 0.60;
        double boardHeight = canvasHeight * 0.88;
        double startX = (canvasWidth - boardWidth) / 2.0;
        double startY = (canvasHeight - boardHeight) / 2.0;
        double cellW = boardWidth / com.touchgrass.bl.games.DodgerLogic.GRID_WIDTH;
        double cellH = boardHeight / com.touchgrass.bl.games.DodgerLogic.GRID_HEIGHT;

        drawBoardFrame(graphics, startX, startY, boardWidth, boardHeight, gameNeon("dodger"), darkTheme);

        graphics.setFill(Color.web("#FF8C42"));
        for (var obstacle : logic.getObstacles()) {
            graphics.fillRoundRect(startX + (obstacle.x() * cellW) + 2, startY + (obstacle.y() * cellH) + 2, cellW - 4, cellH - 4, 6, 6);
        }

        graphics.setFill(Color.web("#00FF87"));
        double px = startX + (logic.getPlayerX() * cellW) + 2;
        double py = startY + ((com.touchgrass.bl.games.DodgerLogic.GRID_HEIGHT - 1) * cellH) + 2;
        graphics.fillRoundRect(px, py, cellW - 4, cellH - 4, 8, 8);
    }

    private void drawMazeIfActive(GraphicsContext graphics, double canvasWidth, double canvasHeight, boolean darkTheme) {
        if (!isMazeEscapeGame() || !(activeSession instanceof LocalSession localSession) || localSession.getMazeEscapeLogic() == null) {
            return;
        }
        var logic = localSession.getMazeEscapeLogic();
        double boardSize = Math.min(canvasWidth * 0.76, canvasHeight * 0.88);
        double startX = (canvasWidth - boardSize) / 2.0;
        double startY = (canvasHeight - boardSize) / 2.0;
        double cell = boardSize / logic.getSize();

        drawBoardFrame(graphics, startX, startY, boardSize, boardSize, gameNeon("maze-escape"), darkTheme);

        graphics.setFill(Color.web("#213B66"));
        for (int row = 0; row < logic.getSize(); row++) {
            for (int col = 0; col < logic.getSize(); col++) {
                if (logic.isWall(row, col)) {
                    graphics.fillRect(startX + (col * cell), startY + (row * cell), cell, cell);
                }
            }
        }

        graphics.setFill(Color.web("#B44FFF"));
        graphics.fillRoundRect(startX + (logic.getGoalCol() * cell) + 3, startY + (logic.getGoalRow() * cell) + 3, cell - 6, cell - 6, 8, 8);
        graphics.setFill(Color.web("#00FF87"));
        graphics.fillRoundRect(startX + (logic.getPlayerCol() * cell) + 3, startY + (logic.getPlayerRow() * cell) + 3, cell - 6, cell - 6, 8, 8);
    }

    private void drawCarRaceIfActive(GraphicsContext graphics, double canvasWidth, double canvasHeight, boolean darkTheme) {
        if (!isCarRaceGame() || !(activeSession instanceof LocalSession localSession) || localSession.getCarRaceLogic() == null) {
            return;
        }
        CarRaceLogic logic = localSession.getCarRaceLogic();
        double boardWidth = Math.min(canvasWidth * 0.42, canvasHeight * 0.58);
        double boardHeight = canvasHeight * 0.90;
        double startX = (canvasWidth - boardWidth) / 2.0;
        double startY = (canvasHeight - boardHeight) / 2.0;
        String neon = gameNeon("car-race");

        drawBoardFrame(graphics, startX, startY, boardWidth, boardHeight, neon, darkTheme);

        double laneWidth = boardWidth / CarRaceLogic.LANES;
        graphics.setStroke(Color.web(neon, 0.45));
        graphics.setLineWidth(2);
        graphics.setLineDashes(14, 14);
        for (int lane = 1; lane < CarRaceLogic.LANES; lane++) {
            double laneX = startX + (lane * laneWidth);
            graphics.strokeLine(laneX, startY + 8, laneX, startY + boardHeight - 8);
        }
        graphics.setLineDashes(0);

        double scaleY = boardHeight / CarRaceLogic.TRACK_HEIGHT;
        double carHeight = CarRaceLogic.CAR_HEIGHT * scaleY;
        double carWidth = laneWidth - 12;

        graphics.setFill(Color.web("#FF4D8F"));
        for (CarRaceLogic.Traffic car : logic.getTraffic()) {
            double tx = startX + (car.lane() * laneWidth) + 6;
            double ty = startY + (car.y() * scaleY);
            graphics.fillRoundRect(tx, ty, carWidth, carHeight, 6, 6);
        }

        graphics.setFill(Color.web(neon, 0.30));
        double playerX = startX + (logic.getPlayerLane() * laneWidth) + 6;
        double playerY = startY + boardHeight - carHeight - 12;
        graphics.fillRoundRect(playerX - 3, playerY - 3, carWidth + 6, carHeight + 6, 9, 9);
        graphics.setFill(Color.web(neon));
        graphics.fillRoundRect(playerX, playerY, carWidth, carHeight, 6, 6);
    }

    private void drawSudokuIfActive(GraphicsContext graphics, double canvasWidth, double canvasHeight, boolean darkTheme) {
        if (!isSudokuGame() || !(activeSession instanceof LocalSession localSession) || localSession.getSudokuLogic() == null) {
            return;
        }
        SudokuLogic logic = localSession.getSudokuLogic();
        double boardSize = Math.min(canvasWidth * 0.70, canvasHeight * 0.92);
        double startX = (canvasWidth - boardSize) / 2.0;
        double startY = (canvasHeight - boardSize) / 2.0;
        double cell = boardSize / SudokuLogic.SIZE;
        String neon = gameNeon("sudoku");

        drawBoardFrame(graphics, startX, startY, boardSize, boardSize, neon, darkTheme);

        if (logic.getSelectedRow() >= 0 && logic.getSelectedCol() >= 0) {
            graphics.setFill(Color.web(neon, 0.18));
            graphics.fillRect(startX + (logic.getSelectedCol() * cell), startY + (logic.getSelectedRow() * cell), cell, cell);
        }

        graphics.setStroke(Color.web(neon, 0.55));
        graphics.setLineWidth(1);
        for (int i = 1; i < SudokuLogic.SIZE; i++) {
            double pos = startX + (i * cell);
            graphics.strokeLine(pos, startY, pos, startY + boardSize);
            pos = startY + (i * cell);
            graphics.strokeLine(startX, pos, startX + boardSize, pos);
        }
        graphics.setLineWidth(2.4);
        graphics.setStroke(Color.web(neon));
        for (int i = 1; i < SudokuLogic.SIZE / SudokuLogic.SUBGRID; i++) {
            double pos = startX + (i * SudokuLogic.SUBGRID * cell);
            graphics.strokeLine(pos, startY, pos, startY + boardSize);
            pos = startY + (i * SudokuLogic.SUBGRID * cell);
            graphics.strokeLine(startX, pos, startX + boardSize, pos);
        }

        graphics.setFont(Font.font("Consolas", Math.max(20, cell * 0.55)));
        for (int row = 0; row < SudokuLogic.SIZE; row++) {
            for (int col = 0; col < SudokuLogic.SIZE; col++) {
                int value = logic.getValue(row, col);
                if (value == 0) {
                    continue;
                }
                Color fill;
                if (logic.isGiven(row, col)) {
                    fill = darkTheme ? Color.web("#E8F4FF") : Color.web("#0D1A36");
                } else if (logic.isCorrect(row, col)) {
                    fill = Color.web(neon);
                } else {
                    fill = Color.web("#FF4D8F");
                }
                graphics.setFill(fill);
                double textX = startX + (col * cell) + (cell * 0.30);
                double textY = startY + (row * cell) + (cell * 0.72);
                graphics.fillText(String.valueOf(value), textX, textY);
            }
        }

        if (logic.isSolved()) {
            graphics.setFill(Color.web(neon, 0.18));
            graphics.fillRect(startX, startY, boardSize, boardSize);
        }
    }

    private void drawShooterIfActive(GraphicsContext graphics, double canvasWidth, double canvasHeight, boolean darkTheme) {
        if (!isShooterGame() || !(activeSession instanceof LocalSession localSession) || localSession.getShooterLogic() == null) {
            return;
        }
        ShooterLogic logic = localSession.getShooterLogic();
        double boardWidth = Math.min(canvasWidth * 0.70, canvasHeight * 1.05);
        double boardHeight = canvasHeight * 0.90;
        double startX = (canvasWidth - boardWidth) / 2.0;
        double startY = (canvasHeight - boardHeight) / 2.0;
        String neon = gameNeon("shooter");

        drawBoardFrame(graphics, startX, startY, boardWidth, boardHeight, neon, darkTheme);

        double scaleX = boardWidth / ShooterLogic.FIELD_WIDTH;
        double scaleY = boardHeight / ShooterLogic.FIELD_HEIGHT;

        graphics.setFill(Color.web(neon, 0.30));
        for (ShooterLogic.Bullet bullet : logic.getBullets()) {
            double bx = startX + (bullet.x() * scaleX);
            double by = startY + (bullet.y() * scaleY);
            graphics.fillRect(bx - 2, by - 2, (ShooterLogic.BULLET_WIDTH * scaleX) + 4, (ShooterLogic.BULLET_HEIGHT * scaleY) + 4);
        }
        graphics.setFill(Color.web(neon));
        for (ShooterLogic.Bullet bullet : logic.getBullets()) {
            double bx = startX + (bullet.x() * scaleX);
            double by = startY + (bullet.y() * scaleY);
            graphics.fillRect(bx, by, ShooterLogic.BULLET_WIDTH * scaleX, ShooterLogic.BULLET_HEIGHT * scaleY);
        }

        graphics.setFill(Color.web("#B44FFF"));
        for (ShooterLogic.Enemy enemy : logic.getEnemies()) {
            double ex = startX + (enemy.x() * scaleX);
            double ey = startY + (enemy.y() * scaleY);
            graphics.fillOval(ex, ey, ShooterLogic.ENEMY_SIZE * scaleX, ShooterLogic.ENEMY_SIZE * scaleY);
        }

        double playerW = ShooterLogic.PLAYER_WIDTH * scaleX;
        double playerH = ShooterLogic.PLAYER_HEIGHT * scaleY;
        double playerX = startX + (logic.getPlayerX() * scaleX);
        double playerY = startY + (ShooterLogic.PLAYER_Y * scaleY);
        graphics.setFill(Color.web(neon, 0.30));
        graphics.fillRoundRect(playerX - 4, playerY - 4, playerW + 8, playerH + 8, 10, 10);
        graphics.setFill(Color.web(neon));
        graphics.fillRoundRect(playerX, playerY, playerW, playerH, 6, 6);
    }

    private void drawDriftTrackIfActive(GraphicsContext graphics, double canvasWidth, double canvasHeight, boolean darkTheme) {
        if (!isDriftTrackGame()) {
            return;
        }
        DriftTrackState state = activeSession.getDriftTrackState();
        if (state == null) {
            graphics.setFill(Color.web("#4A6A8A"));
            graphics.setFont(Font.font("Consolas", 20));
            graphics.fillText("WAITING FOR DRIFT TRACK SYNC...", canvasWidth * 0.27, canvasHeight * 0.52);
            return;
        }

        double trackWidth = Math.min(canvasWidth * 0.78, canvasHeight * 1.55);
        double trackHeight = canvasHeight * 0.90;
        double startX = (canvasWidth - trackWidth) / 2.0;
        double startY = (canvasHeight - trackHeight) / 2.0;
        String neon = gameNeon("drift-track");

        drawBoardFrame(graphics, startX, startY, trackWidth, trackHeight, neon, darkTheme);

        double half = trackWidth / 2.0;
        double laneWidth = half / DriftTrackLogic.LANES_PER_SIDE;
        double scaleY = trackHeight / DriftTrackLogic.TRACK_HEIGHT;
        double carHeight = DriftTrackLogic.CAR_HEIGHT * scaleY;
        double carWidth = laneWidth - 10;

        graphics.setStroke(Color.web(neon, 0.55));
        graphics.setLineWidth(2.6);
        graphics.strokeLine(startX + half, startY + 6, startX + half, startY + trackHeight - 6);

        graphics.setStroke(Color.web(neon, 0.32));
        graphics.setLineWidth(1.6);
        graphics.setLineDashes(12, 12);
        for (int side = 0; side < 2; side++) {
            double sideOriginX = startX + (side * half);
            for (int lane = 1; lane < DriftTrackLogic.LANES_PER_SIDE; lane++) {
                double laneX = sideOriginX + (lane * laneWidth);
                graphics.strokeLine(laneX, startY + 8, laneX, startY + trackHeight - 8);
            }
        }
        graphics.setLineDashes(0);

        graphics.setFill(Color.web("#FF4D8F"));
        for (DriftTrackState.Obstacle obstacle : state.getObstacles()) {
            double sideOriginX = startX + (obstacle.getSide() * half);
            double ox = sideOriginX + (obstacle.getLane() * laneWidth) + 5;
            double oy = startY + (obstacle.getY() * scaleY);
            graphics.fillRoundRect(ox, oy, carWidth, carHeight, 6, 6);
        }

        drawDriftCar(graphics, startX, startY, half, laneWidth, scaleY, carWidth, carHeight, 0, state.getPlayer1Lane(), state.isPlayer1Crashed(), gameNeon("snake"));
        drawDriftCar(graphics, startX, startY, half, laneWidth, scaleY, carWidth, carHeight, 1, state.getPlayer2Lane(), state.isPlayer2Crashed(), gameNeon("pong"));

        graphics.setFont(Font.font("Consolas", Math.max(14, trackHeight * 0.04)));
        graphics.setFill(Color.web(gameNeon("snake")));
        graphics.fillText("P1 " + state.getPlayer1Distance() + " / " + DriftTrackLogic.FINISH_DISTANCE, startX + 14, startY + 26);
        graphics.setFill(Color.web(gameNeon("pong")));
        graphics.fillText("P2 " + state.getPlayer2Distance() + " / " + DriftTrackLogic.FINISH_DISTANCE, startX + half + 14, startY + 26);

        if (state.isFinished() && state.getWinningPlayer() > 0) {
            graphics.setFill(Color.web(neon, 0.20));
            graphics.fillRect(startX, startY, trackWidth, trackHeight);
            graphics.setFill(Color.web("#E8F4FF"));
            graphics.setFont(Font.font("Consolas", Math.max(28, trackHeight * 0.10)));
            graphics.fillText("PLAYER " + state.getWinningPlayer() + " WINS", startX + (trackWidth * 0.30), startY + (trackHeight * 0.50));
        }
    }

    private void drawDriftCar(GraphicsContext graphics, double startX, double startY, double half, double laneWidth, double scaleY,
                              double carWidth, double carHeight, int side, int lane, boolean crashed, String color) {
        double sideOriginX = startX + (side * half);
        double cx = sideOriginX + (lane * laneWidth) + 5;
        double cy = startY + ((DriftTrackLogic.TRACK_HEIGHT - DriftTrackLogic.CAR_HEIGHT - 4) * scaleY);
        graphics.setFill(Color.web(color, crashed ? 0.20 : 0.32));
        graphics.fillRoundRect(cx - 3, cy - 3, carWidth + 6, carHeight + 6, 9, 9);
        graphics.setFill(Color.web(crashed ? "#FF4D8F" : color));
        graphics.fillRoundRect(cx, cy, carWidth, carHeight, 6, 6);
    }

    private void drawBoardFrame(GraphicsContext graphics, double startX, double startY, double width, double height, String neonColor, boolean darkTheme) {
        graphics.setFill(darkTheme ? Color.web("rgba(8,14,30,0.94)") : Color.web("rgba(248,252,255,0.94)"));
        graphics.fillRoundRect(startX - 16, startY - 16, width + 32, height + 32, 24, 24);
        graphics.setStroke(Color.web(neonColor, 0.90));
        graphics.setLineWidth(1.5);
        graphics.strokeRoundRect(startX - 16, startY - 16, width + 32, height + 32, 24, 24);
        graphics.setFill(darkTheme ? Color.web("#081429") : Color.web("#ECF4FF"));
        graphics.fillRoundRect(startX, startY, width, height, 18, 18);
    }

    private boolean shouldTickLogic(long now) {
        if (isPongGame() || isDriftTrackGame()) {
            return true;
        }
        if (isSnakeGame() && activeSession instanceof LocalSession localSession && localSession.getSnakeLogic() != null) {
            long dynamicTick = Math.max(55_000_000L, LOGIC_TICK_NS - (localSession.getSnakeLogic().getScore() * 350_000L));
            return lastLogicTickTime == 0L || now - lastLogicTickTime >= dynamicTick;
        }
        if (isCarRaceGame()) {
            return lastLogicTickTime == 0L || now - lastLogicTickTime >= 30_000_000L;
        }
        if (isShooterGame()) {
            return lastLogicTickTime == 0L || now - lastLogicTickTime >= 25_000_000L;
        }
        if (isSudokuGame()) {
            return lastLogicTickTime == 0L || now - lastLogicTickTime >= 100_000_000L;
        }
        return lastLogicTickTime == 0L || now - lastLogicTickTime >= LOGIC_TICK_NS;
    }

    private boolean isSnakeGame() {
        return GameCatalog.ENGINE_SNAKE.equalsIgnoreCase(GameCatalog.resolveEngineId(gameId));
    }

    private boolean isPongGame() {
        return GameCatalog.ENGINE_PONG.equalsIgnoreCase(GameCatalog.resolveEngineId(gameId));
    }

    private boolean isTicTacToeGame() {
        return GameCatalog.ENGINE_TIC_TAC_TOE.equalsIgnoreCase(GameCatalog.resolveEngineId(gameId));
    }

    private boolean isDodgerGame() {
        return GameCatalog.ENGINE_DODGER.equalsIgnoreCase(GameCatalog.resolveEngineId(gameId));
    }

    private boolean isMazeEscapeGame() {
        return GameCatalog.ENGINE_MAZE_ESCAPE.equalsIgnoreCase(GameCatalog.resolveEngineId(gameId));
    }

    private boolean isCarRaceGame() {
        return GameCatalog.ENGINE_CAR_RACE.equalsIgnoreCase(GameCatalog.resolveEngineId(gameId));
    }

    private boolean isSudokuGame() {
        return GameCatalog.ENGINE_SUDOKU.equalsIgnoreCase(GameCatalog.resolveEngineId(gameId));
    }

    private boolean isShooterGame() {
        return GameCatalog.ENGINE_SHOOTER.equalsIgnoreCase(GameCatalog.resolveEngineId(gameId));
    }

    private boolean isDriftTrackGame() {
        return GameCatalog.ENGINE_DRIFT_TRACK.equalsIgnoreCase(GameCatalog.resolveEngineId(gameId));
    }

    private String gameName() {
        return GameCatalog.getById(gameId).title().toUpperCase();
    }

    private String gameNeon() {
        return GameCatalog.resolveNeon(gameId);
    }

    private boolean isLocalCoOpPong() {
        return isPongGame()
                && activeSession instanceof LocalSession
                && "LocalCoOp".equalsIgnoreCase(activeSession.getMode());
    }

    private boolean isDualPlayerKey(KeyCode keyCode) {
        return keyCode == KeyCode.W || keyCode == KeyCode.S || keyCode == KeyCode.UP || keyCode == KeyCode.DOWN
                || keyCode == KeyCode.A || keyCode == KeyCode.D || keyCode == KeyCode.LEFT || keyCode == KeyCode.RIGHT;
    }

    private void pumpDualPlayerInputs() {
        if (isLocalCoOpPong() && activeSession instanceof LocalSession localPongSession) {
            boolean p1Up = pressedKeys.contains(KeyCode.W);
            boolean p1Down = pressedKeys.contains(KeyCode.S);
            if (p1Up ^ p1Down) {
                localPongSession.handleInputForPlayer(p1Up ? InputCommand.UP : InputCommand.DOWN, 1, true);
            }
            boolean p2Up = pressedKeys.contains(KeyCode.UP);
            boolean p2Down = pressedKeys.contains(KeyCode.DOWN);
            if (p2Up ^ p2Down) {
                localPongSession.handleInputForPlayer(p2Up ? InputCommand.UP : InputCommand.DOWN, 2, true);
            }
            return;
        }
        if (!isDriftTrackGame()) {
            return;
        }
        if (activeSession instanceof LocalSession localDriftSession) {
            if (consumeOnce(KeyCode.A)) {
                localDriftSession.handleInputForPlayer(InputCommand.LEFT, 1, true);
            }
            if (consumeOnce(KeyCode.D)) {
                localDriftSession.handleInputForPlayer(InputCommand.RIGHT, 1, true);
            }
            if (consumeOnce(KeyCode.LEFT)) {
                localDriftSession.handleInputForPlayer(InputCommand.LEFT, 2, true);
            }
            if (consumeOnce(KeyCode.RIGHT)) {
                localDriftSession.handleInputForPlayer(InputCommand.RIGHT, 2, true);
            }
            return;
        }
        if (consumeOnce(KeyCode.A) || consumeOnce(KeyCode.LEFT)) {
            activeSession.handleInput(InputCommand.LEFT, true);
        }
        if (consumeOnce(KeyCode.D) || consumeOnce(KeyCode.RIGHT)) {
            activeSession.handleInput(InputCommand.RIGHT, true);
        }
    }

    private boolean consumeOnce(KeyCode keyCode) {
        return pressedKeys.remove(keyCode);
    }

    private void returnToLobby() {
        handleGameOverAction(false);
    }

    private void handleCanvasClicked(MouseEvent event) {
        if (isSudokuGame() && activeSession instanceof LocalSession localSession && localSession.getSudokuLogic() != null) {
            handleSudokuClick(localSession.getSudokuLogic(), event.getX(), event.getY());
            return;
        }
        if (!isTicTacToeGame() || !(activeSession instanceof LocalSession localSession)) {
            return;
        }
        TicTacToeLogic logic = localSession.getTicTacToeLogic();
        if (logic == null || logic.isGameOver()) {
            return;
        }

        double boardSize = Math.min(canvas.getWidth() * 0.72, canvas.getHeight() * 0.88);
        double startX = (canvas.getWidth() - boardSize) / 2.0;
        double startY = (canvas.getHeight() - boardSize) / 2.0;
        double cellSize = boardSize / TicTacToeLogic.GRID_SIZE;

        double x = event.getX();
        double y = event.getY();
        if (x < startX || x > startX + boardSize || y < startY || y > startY + boardSize) {
            return;
        }

        int col = (int) ((x - startX) / cellSize);
        int row = (int) ((y - startY) / cellSize);
        boolean placed = localSession.placeTicTacToeMark(row, col);
        if (!placed) {
            inlineStatusMessage = "Cell already occupied. Pick another.";
            return;
        }
        inlineStatusMessage = "";
    }

    private void handleSudokuClick(SudokuLogic logic, double clickX, double clickY) {
        double boardSize = Math.min(canvas.getWidth() * 0.70, canvas.getHeight() * 0.92);
        double startX = (canvas.getWidth() - boardSize) / 2.0;
        double startY = (canvas.getHeight() - boardSize) / 2.0;
        if (clickX < startX || clickX > startX + boardSize || clickY < startY || clickY > startY + boardSize) {
            return;
        }
        double cell = boardSize / SudokuLogic.SIZE;
        int col = (int) ((clickX - startX) / cell);
        int row = (int) ((clickY - startY) / cell);
        logic.selectCell(row, col);
        inlineStatusMessage = "Use 1-9 to fill, 0 to clear, H for hint.";
    }

    private void setupGameOverOverlay(boolean darkTheme) {
        Label title = new Label("GAME OVER");
        title.setStyle("-fx-font-size: 38px; -fx-font-weight: 900; -fx-letter-spacing: 3px; -fx-text-fill: " + danger(darkTheme) + ";");
        title.setEffect(new DropShadow(20, Color.web(danger(darkTheme), 0.62)));

        gameOverScoreLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: " + primaryText(darkTheme) + ";");

        Button saveAndQuitButton = new Button("SAVE SCORE");
        saveAndQuitButton.setStyle("-fx-background-color: linear-gradient(to right, #00FF87, #6BFFB8);"
                + "-fx-text-fill: #060D1C; -fx-font-size: 14px; -fx-font-weight: 900;"
                + "-fx-background-radius: 14; -fx-padding: 10 18 10 18;");
        saveAndQuitButton.setEffect(new DropShadow(18, Color.web(gameNeon("snake"), 0.30)));
        saveAndQuitButton.setOnAction(event -> handleGameOverAction(true));

        Button quitButton = new Button("QUIT TO LOBBY");
        quitButton.setStyle("-fx-background-color: rgba(45, 10, 28, 0.90);"
                + "-fx-text-fill: " + danger(darkTheme) + ";"
                + "-fx-font-size: 13px; -fx-font-weight: 900;"
                + "-fx-border-color: " + danger(darkTheme) + ";"
                + "-fx-border-width: 1.2;"
                + "-fx-background-radius: 14;"
                + "-fx-border-radius: 14;"
                + "-fx-padding: 10 18 10 18;");
        quitButton.setOnAction(event -> handleGameOverAction(false));

        HBox actions = new HBox(10, saveAndQuitButton, quitButton);
        actions.setAlignment(Pos.CENTER);

        VBox card = new VBox(10, title, gameOverScoreLabel, actions);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24));
        card.setMaxWidth(420);
        card.setStyle("-fx-background-color: rgba(8,14,30,0.95);"
                + "-fx-background-radius: 26;"
                + "-fx-border-radius: 26;"
                + "-fx-border-width: 1.5;"
                + "-fx-border-color: rgba(255,77,143,0.35);");
        card.setEffect(new DropShadow(36, Color.web(danger(darkTheme), 0.22)));

        gameOverOverlay.getChildren().setAll(card);
        gameOverOverlay.setAlignment(Pos.CENTER);
        gameOverOverlay.setStyle("-fx-background-color: rgba(5,10,22,0.72);");
        gameOverOverlay.setVisible(false);
        gameOverOverlay.setManaged(false);
    }

    private void drawAmbientEffects(GraphicsContext graphics, boolean darkTheme, double canvasWidth, double canvasHeight) {
        if (!systemController.getUiSettings().isAmbientMotion()) {
            return;
        }
        long time = System.nanoTime() / 6_000_000L;
        double waveX = (time % 220) * 4.1;
        double waveY = (time % 180) * 3.0;
        Color glow = darkTheme ? Color.web(gameNeon(), 0.08) : Color.web(gameNeon(), 0.06);
        graphics.setFill(glow);
        graphics.fillOval(waveX - 110, 26, 220, 220);
        graphics.fillOval(canvasWidth - waveX - 120, canvasHeight - 230, 240, 240);
        graphics.fillOval((waveY * 1.7) % canvasWidth, canvasHeight * 0.30, 180, 180);
    }

    private void showGameOverOverlay() {
        gameOverOverlayShown = true;
        gameOverScoreLabel.setText("FINAL SCORE: " + activeSession.getScore());
        gameOverOverlay.setVisible(true);
        gameOverOverlay.setManaged(true);
    }

    private void handleGameOverAction(boolean saveScore) {
        if (gameOverActionTriggered) {
            return;
        }
        gameOverActionTriggered = true;
        stopGameLoop();
        activeSession.end();
        if (stage.getScene() != null) {
            stage.getScene().setOnKeyPressed(null);
            stage.getScene().setOnKeyReleased(null);
        }
        systemController.handleGameOver(gameId, activeSession.getScore(), saveScore);
    }

    private String appBackground(boolean darkTheme) {
        return darkTheme ? "#060D1C" : "#EEF5FF";
    }

    private String topBarBg(boolean darkTheme) {
        return darkTheme ? "rgba(6,13,28,0.97)" : "rgba(240,250,245,0.96)";
    }

    private String navButtonStyle(boolean darkTheme) {
        return "-fx-background-color: " + topBarBg(darkTheme) + ";"
                + "-fx-text-fill: " + primaryText(darkTheme) + ";"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: 900;"
                + "-fx-letter-spacing: 1px;"
                + "-fx-border-color: rgba(0,184,255,0.20);"
                + "-fx-border-width: 1;"
                + "-fx-background-radius: 12;"
                + "-fx-border-radius: 12;"
                + "-fx-padding: 10 14 10 14;";
    }

    private String primaryText(boolean darkTheme) {
        return darkTheme ? "#E8F4FF" : "#0D1A36";
    }

    private String secondaryText(boolean darkTheme) {
        return darkTheme ? "#4A6A8A" : "#607080";
    }

    private String danger(boolean darkTheme) {
        return darkTheme ? "#FF4D8F" : "#CC1060";
    }

    private String gameNeon(String gameId) {
        return GameCatalog.resolveNeon(gameId);
    }
}
