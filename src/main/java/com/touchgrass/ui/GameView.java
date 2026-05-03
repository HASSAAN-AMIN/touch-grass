package com.touchgrass.ui;

import com.touchgrass.bl.LocalSession;
import com.touchgrass.bl.Session;
import com.touchgrass.bl.SystemController;
import com.touchgrass.bl.games.InputCommand;
import com.touchgrass.bl.games.SnakeLogic;
import javafx.animation.AnimationTimer;
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
    private static final double WIDTH = 960;
    private static final double HEIGHT = 600;
    private static final long LOGIC_TICK_NS = 100_000_000L;

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
    private AnimationTimer animationTimer;
    private long lastFrameTime;
    private long lastLogicTickTime;
    private double fps;
    private boolean gameOverOverlayShown;
    private boolean gameOverActionTriggered;

    public GameView(Stage stage, SystemController systemController, String gameId, Session activeSession) {
        this.stage = stage;
        this.systemController = systemController;
        this.gameId = gameId;
        this.activeSession = activeSession;
        this.canvas = new Canvas(WIDTH, HEIGHT);
        this.pressedKeys = ConcurrentHashMap.newKeySet();
        this.root = new BorderPane();
        this.playArea = new StackPane();
        this.gameOverOverlay = new VBox(12);
        this.gameOverScoreLabel = new Label();
    }

    public Parent createRoot() {
        Label sessionLabel = new Label("Session: " + activeSession.getMode());
        sessionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #667085; -fx-font-weight: 600;");

        Button quitButton = new Button("Quit");
        quitButton.setStyle(
                "-fx-background-color: #dce3db; -fx-text-fill: #1f2933; -fx-font-size: 13px;"
                        + "-fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 9 18 9 18;");
        quitButton.setOnAction(event -> returnToLobby());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(10, sessionLabel, spacer, quitButton);
        topBar.setPadding(new Insets(16, 20, 12, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox canvasContainer = new VBox(canvas);
        canvasContainer.setPadding(new Insets(0, 20, 20, 20));
        canvasContainer.setAlignment(Pos.CENTER);

        setupGameOverOverlay();
        playArea.getChildren().setAll(canvasContainer, gameOverOverlay);

        root.setTop(topBar);
        root.setCenter(playArea);
        root.setStyle("-fx-background-color: #F8F9FA;");
        root.setFocusTraversable(true);
        return root;
    }

    public Scene createScene() {
        Scene scene = new Scene(createRoot(), WIDTH, HEIGHT);
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
                if (lastLogicTickTime == 0L || now - lastLogicTickTime >= LOGIC_TICK_NS) {
                    activeSession.tick();
                    lastLogicTickTime = now;
                }
                if (!gameOverOverlayShown && activeSession.isGameOver()) {
                    stopGameLoop();
                    showGameOverOverlay();
                }
                renderFrame(graphics);
            }
        };
        animationTimer.start();
    }

    public void stopGameLoop() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    private void renderFrame(GraphicsContext graphics) {
        graphics.setFill(Color.web("#F8F9FA"));
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        drawSnakeIfActive(graphics);

        graphics.setFill(Color.web("#4B5563"));
        graphics.setFont(Font.font("Consolas", 24));
        graphics.fillText("Game Running: " + Math.round(fps) + " FPS", 32, 44);

        graphics.setFill(Color.web("#6B7280"));
        graphics.setFont(Font.font("Consolas", 16));
        graphics.fillText("Session: " + activeSession.getMode(), 32, 72);
        graphics.fillText("Use WASD + Arrows, press ESC to quit", 32, 96);
    }

    private void handleKeyPressed(KeyEvent event) {
        KeyCode keyCode = event.getCode();
        if (keyCode == KeyCode.ESCAPE) {
            returnToLobby();
            return;
        }
        InputCommand command = toInputCommand(keyCode);
        if (command == null) {
            return;
        }
        if (pressedKeys.add(keyCode)) {
            activeSession.handleInput(command, true);
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        KeyCode keyCode = event.getCode();
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

    private void drawSnakeIfActive(GraphicsContext graphics) {
        if (!(activeSession instanceof LocalSession localSession)) {
            return;
        }
        SnakeLogic snakeLogic = localSession.getSnakeLogic();
        if (snakeLogic == null) {
            return;
        }

        double boardWidth = SnakeLogic.GRID_WIDTH * SnakeLogic.TILE_SIZE;
        double boardHeight = SnakeLogic.GRID_HEIGHT * SnakeLogic.TILE_SIZE;
        double startX = (WIDTH - boardWidth) / 2.0;
        double startY = (HEIGHT - boardHeight) / 2.0 + 24;

        graphics.setFill(Color.web("#FFFFFF"));
        graphics.fillRoundRect(startX - 14, startY - 14, boardWidth + 28, boardHeight + 28, 24, 24);

        graphics.setFill(Color.web("#E9EEF2"));
        graphics.fillRoundRect(startX, startY, boardWidth, boardHeight, 18, 18);

        graphics.setFill(Color.web("#7FB069"));
        for (SnakeLogic.Cell cell : snakeLogic.getSnakeBody()) {
            double x = startX + (cell.x() * SnakeLogic.TILE_SIZE) + 2;
            double y = startY + (cell.y() * SnakeLogic.TILE_SIZE) + 2;
            double size = SnakeLogic.TILE_SIZE - 4;
            graphics.fillRoundRect(x, y, size, size, 10, 10);
        }

        SnakeLogic.Cell food = snakeLogic.getFood();
        double foodX = startX + (food.x() * SnakeLogic.TILE_SIZE) + 2;
        double foodY = startY + (food.y() * SnakeLogic.TILE_SIZE) + 2;
        double foodSize = SnakeLogic.TILE_SIZE - 4;
        graphics.setFill(Color.web("#E55934"));
        graphics.fillRoundRect(foodX, foodY, foodSize, foodSize, 12, 12);

        graphics.setFill(Color.web("#52606D"));
        graphics.setFont(Font.font("Consolas", 16));
        graphics.fillText("Score: " + snakeLogic.getScore(), startX, startY - 18);
        if (snakeLogic.isGameOver()) {
            graphics.setFill(Color.web("#C2410C"));
            graphics.setFont(Font.font("Consolas", 30));
            graphics.fillText("Game Over", startX + 154, startY + (boardHeight / 2));
        }
    }

    private void returnToLobby() {
        handleGameOverAction(false);
    }

    private void setupGameOverOverlay() {
        Label title = new Label("Game Over");
        title.setStyle("-fx-font-size: 34px; -fx-font-weight: 800; -fx-text-fill: #1F2937;");

        gameOverScoreLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: 600; -fx-text-fill: #4B5563;");

        Button saveAndQuitButton = new Button("Save Score & Quit");
        saveAndQuitButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #BDE7C5, #D7C7F7);"
                        + "-fx-text-fill: #1F2937; -fx-font-size: 14px; -fx-font-weight: 700;"
                        + "-fx-background-radius: 12; -fx-padding: 10 20 10 20;");
        saveAndQuitButton.setOnAction(event -> handleGameOverAction(true));

        Button quitButton = new Button("Quit to Lobby");
        quitButton.setStyle(
                "-fx-background-color: #E9EEF2; -fx-text-fill: #374151; -fx-font-size: 14px; -fx-font-weight: 700;"
                        + "-fx-background-radius: 12; -fx-padding: 10 20 10 20;");
        quitButton.setOnAction(event -> handleGameOverAction(false));

        HBox actions = new HBox(12, saveAndQuitButton, quitButton);
        actions.setAlignment(Pos.CENTER);

        VBox card = new VBox(10, title, gameOverScoreLabel, actions);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24));
        card.setMaxWidth(420);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16;");

        gameOverOverlay.getChildren().setAll(card);
        gameOverOverlay.setAlignment(Pos.CENTER);
        gameOverOverlay.setStyle("-fx-background-color: rgba(248, 249, 250, 0.78);");
        gameOverOverlay.setVisible(false);
        gameOverOverlay.setManaged(false);
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
}
