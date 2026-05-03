package com.touchgrass.ui;

import com.touchgrass.bl.SystemController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.stage.Stage;

public final class MainLobbyView {
    private final Stage stage;
    private final SystemController systemController;
    private final StackPane centerStack;
    private final VBox gamesPane;
    private final VBox modePane;
    private final Label selectedGameLabel;
    private String selectedGameId;

    public MainLobbyView(Stage stage, SystemController systemController) {
        this.stage = stage;
        this.systemController = systemController;
        this.centerStack = new StackPane();
        this.gamesPane = new VBox(16);
        this.modePane = new VBox(14);
        this.selectedGameLabel = new Label();
    }

    public Scene createScene() {
        return new Scene(createRoot(), 960, 600);
    }

    public Parent createRoot() {
        Label title = new Label("Touch Grass");
        title.setStyle("-fx-font-size: 30px; -fx-text-fill: #111827; -fx-font-weight: 800;");

        Label subtitle = new Label("Games Library");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");

        VBox titleBox = new VBox(4, title, subtitle);

        Button logoutButton = new Button("Logout");
        logoutButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #B8E1DD, #D5B9FF);"
                        + "-fx-text-fill: #1F2937; -fx-font-size: 13px;"
                        + "-fx-font-weight: 700; -fx-background-radius: 14; -fx-padding: 9 18 9 18;");
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
        sectionLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #334155; -fx-font-weight: 700;");

        setupGamesPane(sectionLabel);
        setupModePane();

        centerStack.getChildren().setAll(gamesPane, modePane);
        showGamesPane();

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(centerStack);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #F8F9FA, #F2F7FF, #F5F3FF);");
        return root;
    }

    private void setupGamesPane(Label sectionLabel) {
        FlowPane grid = new FlowPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.getChildren().addAll(
                createGameCard("Snake", "snake"),
                createGameCard("Pong", "pong"),
                createGameCard("Tic-Tac-Toe", "tic-tac-toe"));

        gamesPane.getChildren().setAll(sectionLabel, grid);
        gamesPane.setPadding(new Insets(0, 24, 24, 24));
        gamesPane.setAlignment(Pos.TOP_LEFT);
    }

    private void setupModePane() {
        Label modeTitle = new Label("Mode Selection");
        modeTitle.setStyle("-fx-font-size: 24px; -fx-text-fill: #1f2933; -fx-font-weight: 800;");

        selectedGameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #667085; -fx-font-weight: 600;");

        Button singlePlayer = createModeButton("Single Player");
        Button localCoOp = createModeButton("Local Co-Op");
        Button lan = createModeButton("LAN Multiplayer");

        Button backButton = new Button("Back");
        backButton.setStyle(
                "-fx-background-color: linear-gradient(to right, #D5EDE3, #E7D8FF);"
                        + "-fx-text-fill: #1f2933; -fx-font-size: 13px;"
                        + "-fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 10 20 10 20;");
        backButton.setOnAction(event -> showGamesPane());

        VBox card = new VBox(12, modeTitle, selectedGameLabel, singlePlayer, localCoOp, lan, backButton);
        card.setPadding(new Insets(24));
        card.setMaxWidth(360);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16;");
        card.setEffect(new DropShadow(16, Color.rgb(16, 24, 40, 0.10)));

        modePane.getChildren().setAll(card);
        modePane.setAlignment(Pos.TOP_CENTER);
        modePane.setPadding(new Insets(20, 24, 24, 24));
    }

    private Button createModeButton(String modeLabel) {
        Button button = new Button(modeLabel);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(
                "-fx-background-color: linear-gradient(to right, #C5E4C9, #D7C7F7);"
                        + "-fx-text-fill: #1f2933; -fx-font-size: 14px;"
                        + "-fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 11 16 11 16;");
        button.setOnAction(event -> systemController.launchGame(selectedGameId, modeLabel));
        return button;
    }

    private Button createGameCard(String gameTitle, String gameId) {
        Button card = new Button(gameTitle);
        card.setPrefSize(240, 170);
        card.setStyle(cardStyle(gameTitle));
        card.setEffect(new DropShadow(14, Color.rgb(16, 24, 40, 0.10)));
        card.setOnAction(event -> showModePane(gameTitle, gameId));
        return card;
    }

    private String cardStyle(String gameTitle) {
        String tone = switch (gameTitle) {
            case "Snake" -> "#D8F2D2";
            case "Pong" -> "#D9EBFF";
            default -> "#F1E2FF";
        };
        return "-fx-background-color: linear-gradient(to bottom right, #FFFFFF, " + tone + ");"
                + "-fx-text-fill: #1f2933;"
                + "-fx-font-size: 22px;"
                + "-fx-font-weight: 700;"
                + "-fx-background-radius: 16;"
                + "-fx-padding: 12 12 12 12;";
    }

    private void showModePane(String gameTitle, String gameId) {
        selectedGameId = gameId;
        selectedGameLabel.setText("Selected Game: " + gameTitle);
        gamesPane.setVisible(false);
        gamesPane.setManaged(false);
        modePane.setVisible(true);
        modePane.setManaged(true);
    }

    private void showGamesPane() {
        modePane.setVisible(false);
        modePane.setManaged(false);
        gamesPane.setVisible(true);
        gamesPane.setManaged(true);
    }
}
