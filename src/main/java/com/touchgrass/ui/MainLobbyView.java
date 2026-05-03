package com.touchgrass.ui;

import com.touchgrass.bl.SystemController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public final class MainLobbyView {
    private final Stage stage;
    private final SystemController systemController;

    public MainLobbyView(Stage stage, SystemController systemController) {
        this.stage = stage;
        this.systemController = systemController;
    }

    public Scene createScene() {
        Label title = new Label("Touch Grass");
        title.setStyle("-fx-font-size: 30px; -fx-text-fill: #f5f7ff; -fx-font-weight: 800;");

        Label subtitle = new Label("Games Library");
        subtitle.setStyle("-fx-font-size: 15px; -fx-text-fill: #9ea7d6;");

        VBox titleBox = new VBox(4, title, subtitle);

        Button logoutButton = new Button("Logout");
        logoutButton.setStyle(
                "-fx-background-color: #2f365f; -fx-text-fill: #f5f7ff; -fx-font-size: 13px;"
                        + "-fx-font-weight: 600; -fx-background-radius: 10; -fx-padding: 9 18 9 18;");
        logoutButton.setOnAction(event -> {
            LoginView loginView = new LoginView(stage, systemController);
            stage.setScene(loginView.createScene());
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(12, titleBox, spacer, logoutButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(24, 24, 16, 24));

        Label sectionLabel = new Label("Browse Games");
        sectionLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #d7ddff; -fx-font-weight: 700;");

        HBox cardRow = new HBox(
                18,
                createGameCard("Snake", "snake"),
                createGameCard("Pong", "pong"),
                createGameCard("Tic-Tac-Toe", "tic-tac-toe"));
        cardRow.setAlignment(Pos.CENTER_LEFT);

        VBox libraryBox = new VBox(16, sectionLabel, cardRow);
        libraryBox.setPadding(new Insets(0, 24, 24, 24));
        libraryBox.setAlignment(Pos.TOP_LEFT);

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(libraryBox);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b0d19, #131a31, #1c2445);");

        return new Scene(root, 960, 600);
    }

    private Button createGameCard(String gameTitle, String gameId) {
        Button card = new Button(gameTitle);
        card.setPrefSize(220, 140);
        card.setStyle(
                "-fx-background-color: rgba(22, 29, 58, 0.95);"
                        + "-fx-text-fill: #f5f7ff;"
                        + "-fx-font-size: 20px;"
                        + "-fx-font-weight: 700;"
                        + "-fx-background-radius: 16;"
                        + "-fx-border-radius: 16;"
                        + "-fx-border-color: rgba(122, 141, 255, 0.3);"
                        + "-fx-border-width: 1.2;");
        card.setOnAction(event -> showModeSelection(gameId));
        return card;
    }

    private void showModeSelection(String gameId) {
        List<String> modes = List.of("Single Player", "Local Co-Op", "LAN Multiplayer");
        ChoiceDialog<String> modeDialog = new ChoiceDialog<>(modes.get(0), modes);
        modeDialog.setTitle("Select Mode");
        modeDialog.setHeaderText("Choose your session mode");
        modeDialog.setContentText("Mode:");

        Optional<String> selectedMode = modeDialog.showAndWait();
        selectedMode.ifPresent(mode -> systemController.launchGame(gameId, mode));
    }
}
