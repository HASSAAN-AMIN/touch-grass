package com.touchgrass.ui;

import com.touchgrass.bl.Session;
import com.touchgrass.bl.SystemController;
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
import javafx.scene.layout.VBox;
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
    private final BorderPane root;
    private AnimationTimer animationTimer;
    private long lastFrameTime;
    private double fps;

    public GameView(Stage stage, SystemController systemController, Session activeSession) {
        this.stage = stage;
        this.systemController = systemController;
        this.activeSession = activeSession;
        this.canvas = new Canvas(WIDTH, HEIGHT);
        this.pressedKeys = ConcurrentHashMap.newKeySet();
        this.root = new BorderPane();
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

        root.setTop(topBar);
        root.setCenter(canvasContainer);
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
        graphics.setFill(Color.web("#FFFFFF"));
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        graphics.setFill(Color.web("#6f7f9a"));
        graphics.setFont(Font.font("Consolas", 26));
        graphics.fillText("Game Running: " + Math.round(fps) + " FPS", 32, 48);

        graphics.setFill(Color.web("#98a2b3"));
        graphics.setFont(Font.font("Consolas", 16));
        graphics.fillText("Session: " + activeSession.getMode(), 32, 82);
        graphics.fillText("Use WASD + Arrows, press ESC to quit", 32, 112);
    }

    private void handleKeyPressed(KeyEvent event) {
        KeyCode keyCode = event.getCode();
        if (keyCode == KeyCode.ESCAPE) {
            returnToLobby();
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

    private void returnToLobby() {
        stopGameLoop();
        activeSession.end();
        MainLobbyView mainLobbyView = new MainLobbyView(stage, systemController);
        if (stage.getScene() == null) {
            stage.setScene(mainLobbyView.createScene());
            return;
        }
        stage.getScene().setRoot(mainLobbyView.createRoot());
    }
}
