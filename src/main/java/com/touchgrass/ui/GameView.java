package com.touchgrass.ui;

import com.touchgrass.bl.LocalSession;
import com.touchgrass.bl.Session;
import com.touchgrass.bl.SystemController;
import com.touchgrass.bl.UiSettings;
import com.touchgrass.bl.games.GameState;
import com.touchgrass.bl.games.InputCommand;
import com.touchgrass.bl.games.PongLogic;
import com.touchgrass.bl.games.SnakeLogic;
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
        UiSettings uiSettings = systemController.getUiSettings();
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;

        hudPrimaryLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: " + (lightTheme ? "#1E293B" : "#E2E8F0") + ";");
        hudScoreLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + (lightTheme ? "#475569" : "#9FB1CD") + ";");
        hudHintLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#8EA0BF") + ";");
        hudStatusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #C81E5B;");

        Button quitButton = new Button("Quit");
        quitButton.setStyle(secondaryButtonStyle(uiSettings));
        quitButton.setOnAction(event -> returnToLobby());

        VBox leftHud = new VBox(4, hudPrimaryLabel, hudScoreLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(12, leftHud, spacer, quitButton);
        topBar.setPadding(new Insets(14, 18, 8, 18));
        topBar.setAlignment(Pos.CENTER_LEFT);

        canvas.setOnMouseClicked(this::handleCanvasClicked);
        VBox canvasContainer = new VBox(canvas);
        canvasContainer.setPadding(new Insets(8, 20, 8, 20));
        canvasContainer.setAlignment(Pos.CENTER);

        setupGameOverOverlay();
        playArea.getChildren().setAll(canvasContainer, gameOverOverlay);

        HBox bottomBar = new HBox(18, hudHintLabel, hudStatusLabel);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(2, 20, 14, 20));

        root.setTop(topBar);
        root.setCenter(playArea);
        root.setBottom(bottomBar);
        root.setStyle("-fx-background-color: " + (lightTheme ? "#EEF2FA" : "#0B1324") + ";");
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
                    pumpLocalCoOpPongInputs();
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
        UiSettings uiSettings = systemController.getUiSettings();
        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
        graphics.setFill(uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT
                ? Color.web("#EEF2FA")
                : Color.web("#0B1324"));
        graphics.fillRect(0, 0, canvasWidth, canvasHeight);
        drawAmbientEffects(graphics, uiSettings, canvasWidth, canvasHeight);

        drawPongIfActive(graphics, canvasWidth, canvasHeight);
        drawSnakeIfActive(graphics, canvasWidth, canvasHeight);
        drawTicTacToeIfActive(graphics, canvasWidth, canvasHeight);
    }

    private void updateHud() {
        UiSettings uiSettings = systemController.getUiSettings();
        hudPrimaryLabel.setText(gameName() + " - " + activeSession.getMode());

        String scoreText;
        if (isSnakeGame() && activeSession instanceof LocalSession localSession && localSession.getSnakeLogic() != null) {
            scoreText = "Score: " + localSession.getSnakeLogic().getScore()
                    + " | Length: " + localSession.getSnakeLogic().getSnakeBody().size();
        } else if (isPongGame()) {
            GameState state = activeSession.getCurrentGameState();
            scoreText = state == null
                    ? "Syncing..."
                    : "Score " + state.scorePlayer1() + " : " + state.scorePlayer2();
        } else if (isTicTacToeGame() && activeSession instanceof LocalSession localSession && localSession.getTicTacToeLogic() != null) {
            TicTacToeLogic logic = localSession.getTicTacToeLogic();
            scoreText = logic.isGameOver()
                    ? (logic.isDraw() ? "Result: Draw" : "Winner: " + logic.getWinner())
                    : "Turn: " + logic.getCurrentPlayer();
        } else {
            scoreText = "Session running";
        }
        if (uiSettings.isShowFps()) {
            scoreText += " | " + Math.round(fps) + " FPS";
        }
        hudScoreLabel.setText(scoreText);
        hudHintLabel.setText("Controls: WASD + Arrows | P = Pause | ESC = Quit");

        if (paused) {
            hudStatusLabel.setText("Paused");
        } else if (!inlineStatusMessage.isBlank()) {
            hudStatusLabel.setText(inlineStatusMessage);
        } else {
            hudStatusLabel.setText("");
        }
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
        if (isLocalCoOpPong()) {
            if (isLocalCoOpPongKey(keyCode)) {
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
        if (isLocalCoOpPong()) {
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

    private void drawSnakeIfActive(GraphicsContext graphics, double canvasWidth, double canvasHeight) {
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

        graphics.setFill(Color.web("#F9FBFF"));
        graphics.fillRoundRect(startX - 16, startY - 16, boardSize + 32, boardSize + 32, 26, 26);
        graphics.setFill(Color.web("#E8EEF7"));
        graphics.fillRoundRect(startX, startY, boardSize, boardSize, 18, 18);

        graphics.setStroke(Color.web("#D6E1F0"));
        graphics.setLineWidth(1);
        for (int i = 1; i < SnakeLogic.GRID_WIDTH; i++) {
            double x = startX + (i * tileSize);
            graphics.strokeLine(x, startY, x, startY + boardSize);
            graphics.strokeLine(startX, x - startX + startY, startX + boardSize, x - startX + startY);
        }

        graphics.setFill(Color.web("#6CA95A"));
        for (SnakeLogic.Cell cell : snakeLogic.getSnakeBody()) {
            double x = startX + (cell.x() * tileSize) + 2;
            double y = startY + (cell.y() * tileSize) + 2;
            double size = tileSize - 4;
            graphics.fillRoundRect(x, y, size, size, 8, 8);
        }

        SnakeLogic.Cell head = snakeLogic.getSnakeBody().isEmpty() ? null : snakeLogic.getSnakeBody().get(0);
        if (head != null) {
            graphics.setFill(Color.web("#4F8E43"));
            double hx = startX + (head.x() * tileSize) + 1.5;
            double hy = startY + (head.y() * tileSize) + 1.5;
            double hSize = tileSize - 3;
            graphics.fillRoundRect(hx, hy, hSize, hSize, 9, 9);
        }

        SnakeLogic.Cell food = snakeLogic.getFood();
        double foodX = startX + (food.x() * tileSize) + 2;
        double foodY = startY + (food.y() * tileSize) + 2;
        double foodSize = tileSize - 4;
        graphics.setFill(Color.web("#E55934"));
        graphics.fillRoundRect(foodX, foodY, foodSize, foodSize, 10, 10);
    }

    private void drawPongIfActive(GraphicsContext graphics, double canvasWidth, double canvasHeight) {
        if (!isPongGame()) {
            return;
        }

        GameState state = activeSession.getCurrentGameState();
        if (state == null) {
            graphics.setFill(Color.web("#64748B"));
            graphics.setFont(Font.font("Consolas", 20));
            graphics.fillText("Waiting for synced game state...", canvasWidth * 0.30, canvasHeight * 0.52);
            return;
        }

        double boardPadding = 18;
        double scale = Math.min((canvasWidth - (boardPadding * 2)) / PongLogic.FIELD_WIDTH,
                (canvasHeight - (boardPadding * 2)) / PongLogic.FIELD_HEIGHT);
        double drawWidth = PongLogic.FIELD_WIDTH * scale;
        double drawHeight = PongLogic.FIELD_HEIGHT * scale;
        double startX = (canvasWidth - drawWidth) / 2.0;
        double startY = (canvasHeight - drawHeight) / 2.0;

        graphics.setFill(Color.web("#F9FBFF"));
        graphics.fillRoundRect(startX - 16, startY - 16, drawWidth + 32, drawHeight + 32, 26, 26);
        graphics.setFill(Color.web("#EEF3FA"));
        graphics.fillRoundRect(startX, startY, drawWidth, drawHeight, 18, 18);

        graphics.setStroke(Color.web("#BAC8DC"));
        graphics.setLineWidth(Math.max(2, 3 * scale));
        graphics.setLineDashes(12 * scale, 12 * scale);
        double centerX = startX + (drawWidth / 2.0);
        graphics.strokeLine(centerX, startY + (12 * scale), centerX, startY + drawHeight - (12 * scale));
        graphics.setLineDashes(0);

        graphics.setFill(Color.web("#7FB069"));
        graphics.fillRoundRect(
                startX + (24 * scale),
                startY + (state.paddle1Y() * scale),
                PongLogic.PADDLE_WIDTH * scale,
                PongLogic.PADDLE_HEIGHT * scale,
                10 * scale,
                10 * scale);
        graphics.setFill(Color.web("#D1B3FF"));
        graphics.fillRoundRect(
                startX + ((PongLogic.FIELD_WIDTH - 24 - PongLogic.PADDLE_WIDTH) * scale),
                startY + (state.paddle2Y() * scale),
                PongLogic.PADDLE_WIDTH * scale,
                PongLogic.PADDLE_HEIGHT * scale,
                10 * scale,
                10 * scale);

        graphics.setFill(Color.web("#E55934"));
        graphics.fillRoundRect(
                startX + (state.ballX() * scale),
                startY + (state.ballY() * scale),
                PongLogic.BALL_SIZE * scale,
                PongLogic.BALL_SIZE * scale,
                10 * scale,
                10 * scale);
    }

    private void drawTicTacToeIfActive(GraphicsContext graphics, double canvasWidth, double canvasHeight) {
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

        graphics.setFill(Color.web("#F9FBFF"));
        graphics.fillRoundRect(startX - 16, startY - 16, boardSize + 32, boardSize + 32, 26, 26);
        graphics.setFill(Color.web("#EEF2F7"));
        graphics.fillRoundRect(startX, startY, boardSize, boardSize, 18, 18);

        graphics.setStroke(Color.web("#A8B3C2"));
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
                graphics.setFill(value == 'X' ? Color.web("#7FB069") : Color.web("#6EA8FE"));
                double textX = startX + (col * cellSize) + (cellSize * 0.34);
                double textY = startY + (row * cellSize) + (cellSize * 0.70);
                graphics.fillText(String.valueOf(value), textX, textY);
            }
        }
    }

    private boolean shouldTickLogic(long now) {
        if (isPongGame()) {
            return true;
        }
        if (isSnakeGame() && activeSession instanceof LocalSession localSession && localSession.getSnakeLogic() != null) {
            long dynamicTick = Math.max(55_000_000L, LOGIC_TICK_NS - (localSession.getSnakeLogic().getScore() * 350_000L));
            return lastLogicTickTime == 0L || now - lastLogicTickTime >= dynamicTick;
        }
        return lastLogicTickTime == 0L || now - lastLogicTickTime >= LOGIC_TICK_NS;
    }

    private boolean isSnakeGame() {
        return "snake".equalsIgnoreCase(gameId);
    }

    private boolean isPongGame() {
        return "pong".equalsIgnoreCase(gameId);
    }

    private boolean isTicTacToeGame() {
        return "tic-tac-toe".equalsIgnoreCase(gameId);
    }

    private String gameName() {
        if (isSnakeGame()) {
            return "Snake";
        }
        if (isPongGame()) {
            return "Pong";
        }
        if (isTicTacToeGame()) {
            return "Tic-Tac-Toe";
        }
        return "Game";
    }

    private boolean isLocalCoOpPong() {
        return isPongGame()
                && activeSession instanceof LocalSession
                && "LocalCoOp".equalsIgnoreCase(activeSession.getMode());
    }

    private boolean isLocalCoOpPongKey(KeyCode keyCode) {
        return keyCode == KeyCode.W || keyCode == KeyCode.S || keyCode == KeyCode.UP || keyCode == KeyCode.DOWN;
    }

    private void pumpLocalCoOpPongInputs() {
        if (!(activeSession instanceof LocalSession localSession) || !isLocalCoOpPong()) {
            return;
        }

        boolean p1Up = pressedKeys.contains(KeyCode.W);
        boolean p1Down = pressedKeys.contains(KeyCode.S);
        if (p1Up ^ p1Down) {
            localSession.handleInputForPlayer(p1Up ? InputCommand.UP : InputCommand.DOWN, 1, true);
        }

        boolean p2Up = pressedKeys.contains(KeyCode.UP);
        boolean p2Down = pressedKeys.contains(KeyCode.DOWN);
        if (p2Up ^ p2Down) {
            localSession.handleInputForPlayer(p2Up ? InputCommand.UP : InputCommand.DOWN, 2, true);
        }
    }

    private void returnToLobby() {
        handleGameOverAction(false);
    }

    private void handleCanvasClicked(MouseEvent event) {
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

    private void setupGameOverOverlay() {
        Label title = new Label("Game Over");
        title.setStyle("-fx-font-size: 34px; -fx-font-weight: 800; -fx-text-fill: #1F2937;");
        gameOverScoreLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: 600; -fx-text-fill: #4B5563;");

        Button saveAndQuitButton = new Button("Save Score & Quit");
        saveAndQuitButton.setStyle("-fx-background-color: linear-gradient(to right, #B8E9CF, #D8C2FF);"
                + "-fx-text-fill: #0F172A; -fx-font-size: 14px; -fx-font-weight: 700;"
                + "-fx-background-radius: 14; -fx-padding: 10 18 10 18;");
        saveAndQuitButton.setOnAction(event -> handleGameOverAction(true));

        Button quitButton = new Button("Quit to Lobby");
        quitButton.setStyle("-fx-background-color: #E3EBF7; -fx-text-fill: #334155; -fx-font-size: 14px; -fx-font-weight: 700;"
                + "-fx-background-radius: 14; -fx-padding: 10 18 10 18;");
        quitButton.setOnAction(event -> handleGameOverAction(false));

        HBox actions = new HBox(10, saveAndQuitButton, quitButton);
        actions.setAlignment(Pos.CENTER);

        VBox card = new VBox(10, title, gameOverScoreLabel, actions);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24));
        card.setMaxWidth(420);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.90); -fx-background-radius: 20;");

        gameOverOverlay.getChildren().setAll(card);
        gameOverOverlay.setAlignment(Pos.CENTER);
        gameOverOverlay.setStyle("-fx-background-color: rgba(238, 242, 250, 0.72);");
        gameOverOverlay.setVisible(false);
        gameOverOverlay.setManaged(false);
    }

    private void drawAmbientEffects(GraphicsContext graphics, UiSettings uiSettings, double canvasWidth, double canvasHeight) {
        if (!uiSettings.isAmbientMotion()) {
            return;
        }
        long time = System.nanoTime() / 6_000_000L;
        double waveX = (time % 220) * 4.1;
        double waveY = (time % 180) * 3.0;
        Color glow = switch (uiSettings.getAccentStyle()) {
            case LAVENDER -> Color.web("#CBB7FF", 0.11);
            case CORAL -> Color.web("#FFC2B3", 0.11);
            default -> Color.web("#BDE7C5", 0.11);
        };
        graphics.setFill(glow);
        graphics.fillOval(waveX - 110, 26, 220, 220);
        graphics.fillOval(canvasWidth - waveX - 120, canvasHeight - 230, 240, 240);
        graphics.fillOval((waveY * 1.7) % canvasWidth, canvasHeight * 0.30, 180, 180);
    }

    private void showGameOverOverlay() {
        gameOverOverlayShown = true;
        gameOverScoreLabel.setText("Final Score: " + activeSession.getScore());
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

    private String secondaryButtonStyle(UiSettings uiSettings) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        return "-fx-background-color: " + (lightTheme ? "#DCE5F5" : "#253650") + ";"
                + "-fx-text-fill: " + (lightTheme ? "#334155" : "#D0D9E8") + ";"
                + "-fx-font-size: 13px; -fx-font-weight: 700;"
                + "-fx-background-radius: 14; -fx-padding: 9 16 9 16;";
    }
}
