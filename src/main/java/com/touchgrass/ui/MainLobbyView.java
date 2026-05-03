package com.touchgrass.ui;

import com.touchgrass.bl.SystemController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class MainLobbyView {
    private final Stage stage;
    private final SystemController systemController;

    public MainLobbyView(Stage stage, SystemController systemController) {
        this.stage = stage;
        this.systemController = systemController;
    }

    public Scene createScene() {
        Label title = new Label("Main Lobby");
        title.setStyle("-fx-font-size: 30px; -fx-text-fill: #f5f7ff; -fx-font-weight: 700;");

        Label subtitle = new Label("Welcome to Touch Grass");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #a8b0d6;");

        Button logoutButton = new Button("Logout");
        logoutButton.setStyle(
                "-fx-background-color: #2f375f; -fx-text-fill: #f5f7ff; -fx-font-size: 14px;"
                        + "-fx-font-weight: 600; -fx-background-radius: 10; -fx-padding: 10 22 10 22;");
        logoutButton.setOnAction(event -> {
            LoginView loginView = new LoginView(stage, systemController);
            stage.setScene(loginView.createScene());
        });

        VBox root = new VBox(16, title, subtitle, logoutButton);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0d1020, #171b33, #1d2240);");

        return new Scene(root, 960, 600);
    }
}
