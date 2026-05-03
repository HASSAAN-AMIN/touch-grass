package com.touchgrass.ui;

import com.touchgrass.bl.LocalSession;
import com.touchgrass.bl.Session;
import com.touchgrass.bl.SystemController;
import com.touchgrass.bl.UiSettings;
import com.touchgrass.bl.games.GameState;
import com.touchgrass.bl.games.InputCommand;
import com.touchgrass.bl.games.PongLogic;
import com.touchgrass.bl.games.SnakeLogic;
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

        String scoreText;
        if (isSnakeGame() && activeSession instanceof LocalSession localSession && localSession.getSnakeLogic() != null) {
            scoreText = "SCORE " + localSession.getSnakeLogic().getScore() + " | LENGTH " + localSession.getSnakeLogic().getSnakeBody().size();
        } else if (isPongGame()) {
            GameState state = activeSession.getCurrentGameState();
            scoreText = state == null
                    ? "SYNCING..."
                    : "P1 " + state.scorePlayer1() + "  :  P2 " + state.scorePlayer2();
        } else if (isTicTacToeGame() && activeSession instanceof LocalSession localSession && localSession.getTicTacToeLogic() != null) {
            TicTacToeLogic logic = localSession.getTicTacToeLogic();
            scoreText = logic.isGameOver()
                    ? (logic.isDraw() ? "RESULT DRAW" : "WINNER " + logic.getWinner())
                    : "TURN " + logic.getCurrentPlayer();
        } else {
            scoreText = "SESSION ACTIVE";
        }
        if (uiSettings.isShowFps()) {
            scoreText += " | " + Math.round(fps) + " FPS";
        }
        hudScoreLabel.setText(scoreText);
        hudHintLabel.setText("WASD + ARROWS | P PAUSE | ESC QUIT");

        if (paused) {
            hudStatusLabel.setText("PAUSED");
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

        graphics.setFill(darkTheme ? Color.web("rgba(8,14,30,0.94)") : Color.web("rgba(248,252,255,0.94)"));
        graphics.fillRoundRect(startX - 16, startY - 16, boardSize + 32, boardSize + 32, 24, 24);
        graphics.setStroke(Color.web(gameNeon("snake"), 0.92));
        graphics.setLineWidth(1.5);
        graphics.strokeRoundRect(startX - 16, startY - 16, boardSize + 32, boardSize + 32, 24, 24);

        graphics.setFill(darkTheme ? Color.web("#081429") : Color.web("#ECF4FF"));
        graphics.fillRoundRect(startX, startY, boardSize, boardSize, 18, 18);

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

        graphics.setFill(darkTheme ? Color.web("rgba(8,14,30,0.94)") : Color.web("rgba(248,252,255,0.94)"));
        graphics.fillRoundRect(startX - 16, startY - 16, drawWidth + 32, drawHeight + 32, 24, 24);
        graphics.setStroke(Color.web(gameNeon("pong"), 0.90));
        graphics.setLineWidth(1.5);
        graphics.strokeRoundRect(startX - 16, startY - 16, drawWidth + 32, drawHeight + 32, 24, 24);

        graphics.setFill(darkTheme ? Color.web("#081429") : Color.web("#ECF4FF"));
        graphics.fillRoundRect(startX, startY, drawWidth, drawHeight, 18, 18);

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

        graphics.setFill(darkTheme ? Color.web("rgba(8,14,30,0.94)") : Color.web("rgba(248,252,255,0.94)"));
        graphics.fillRoundRect(startX - 16, startY - 16, boardSize + 32, boardSize + 32, 24, 24);
        graphics.setStroke(Color.web(gameNeon("tic-tac-toe"), 0.90));
        graphics.setLineWidth(1.5);
        graphics.strokeRoundRect(startX - 16, startY - 16, boardSize + 32, boardSize + 32, 24, 24);

        graphics.setFill(darkTheme ? Color.web("#081429") : Color.web("#ECF4FF"));
        graphics.fillRoundRect(startX, startY, boardSize, boardSize, 18, 18);

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
        return GameCatalog.ENGINE_SNAKE.equalsIgnoreCase(GameCatalog.resolveEngineId(gameId));
    }

    private boolean isPongGame() {
        return GameCatalog.ENGINE_PONG.equalsIgnoreCase(GameCatalog.resolveEngineId(gameId));
    }

    private boolean isTicTacToeGame() {
        return GameCatalog.ENGINE_TIC_TAC_TOE.equalsIgnoreCase(GameCatalog.resolveEngineId(gameId));
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
