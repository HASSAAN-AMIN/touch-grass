package com.touchgrass.ui;

import com.touchgrass.bl.Session;
import com.touchgrass.bl.SystemController;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GameView {
    private static final double WIDTH = 960;
    private static final double HEIGHT = 600;

    private final Stage stage;
    private final SystemController systemController;
    private final Session activeSession;
    private final Canvas canvas;
    private final Set<KeyCode> pressedKeys;
    private AnimationTimer animationTimer;
    private long lastFrameTime;
    private double fps;

    public GameView(Stage stage, SystemController systemController, Session activeSession) {
        this.stage = stage;
        this.systemController = systemController;
        this.activeSession = activeSession;
        this.canvas = new Canvas(WIDTH, HEIGHT);
        this.pressedKeys = ConcurrentHashMap.newKeySet();
    }

    public Scene createScene() {
        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: #0a0c16;");

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        scene.addEventHandler(KeyEvent.KEY_RELEASED, this::handleKeyReleased);
        return scene;
    }

    public void startGameLoop() {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
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
        graphics.setFill(Color.web("#0a0c16"));
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        graphics.setFill(Color.web("#72a0ff"));
        graphics.setFont(Font.font("Consolas", 26));
        graphics.fillText("Game Running: " + Math.round(fps) + " FPS", 32, 48);

        graphics.setFill(Color.web("#9ba8d8"));
        graphics.setFont(Font.font("Consolas", 16));
        graphics.fillText("Session: " + activeSession.getMode(), 32, 82);
        graphics.fillText("Press ESC to return to lobby", 32, 112);
    }

    private void handleKeyPressed(KeyEvent event) {
        KeyCode keyCode = event.getCode();
        if (keyCode == KeyCode.ESCAPE) {
            stopGameLoop();
            MainLobbyView mainLobbyView = new MainLobbyView(stage, systemController);
            stage.setScene(mainLobbyView.createScene());
            return;
        }
        if (!isTrackedKey(keyCode)) {
            return;
        }
        if (pressedKeys.add(keyCode)) {
            activeSession.handleInput(keyCode.getName(), true);
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        KeyCode keyCode = event.getCode();
        if (!isTrackedKey(keyCode)) {
            return;
        }
        if (pressedKeys.remove(keyCode)) {
            activeSession.handleInput(keyCode.getName(), false);
        }
    }

    private boolean isTrackedKey(KeyCode keyCode) {
        return keyCode == KeyCode.W
                || keyCode == KeyCode.A
                || keyCode == KeyCode.S
                || keyCode == KeyCode.D
                || keyCode == KeyCode.UP
                || keyCode == KeyCode.DOWN
                || keyCode == KeyCode.LEFT
                || keyCode == KeyCode.RIGHT;
    }
}
