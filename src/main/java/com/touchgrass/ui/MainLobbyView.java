package com.touchgrass.ui;

import com.touchgrass.bl.SystemController;
import com.touchgrass.bl.UiSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class MainLobbyView {
    private static final String[] DAILY_TIPS = {
            "Tip: Press ESC in-game for a quick exit.",
            "Tip: Snake scores +10 per food.",
            "Tip: LAN host is authoritative in Pong.",
            "Tip: Save scores to climb the leaderboard.",
            "Tip: Try Local Co-Op Pong with a friend."
    };

    private final Stage stage;
    private final SystemController systemController;
    private final StackPane centerStack;
    private final VBox gamesPane;
    private final VBox modePane;
    private final VBox lanPane;
    private final VBox leaderboardPane;
    private final VBox settingsPane;
    private final Label selectedGameLabel;
    private final Label lanStatusLabel;
    private final Label globalStatusLabel;
    private final TextField lanIpField;
    private String selectedGameId;
    private String selectedGameTitle;

    public MainLobbyView(Stage stage, SystemController systemController) {
        this.stage = stage;
        this.systemController = systemController;
        this.centerStack = new StackPane();
        this.gamesPane = new VBox(16);
        this.modePane = new VBox(14);
        this.lanPane = new VBox(14);
        this.leaderboardPane = new VBox(14);
        this.settingsPane = new VBox(14);
        this.selectedGameLabel = new Label();
        this.lanStatusLabel = new Label();
        this.globalStatusLabel = new Label();
        this.lanIpField = new TextField();
    }

    public Scene createScene() {
        return new Scene(createRoot(), 960, 600);
    }

    public Parent createRoot() {
        UiSettings uiSettings = systemController.getUiSettings();
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;

        systemController.setStatusMessageListener(message -> {
            lanStatusLabel.setText(message);
            globalStatusLabel.setText(message);
        });

        Label title = new Label("Touch Grass");
        title.setStyle("-fx-font-size: 30px; -fx-text-fill: " + (lightTheme ? "#111827" : "#E2E8F0") + "; -fx-font-weight: 800;");

        Label subtitle = new Label("Games Library");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#8EA0BF") + ";");
        globalStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #E11D48; -fx-font-weight: 700;");
        Label tipLabel = new Label(pickTip());
        tipLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (lightTheme ? "#667085" : "#A3B4D1") + "; -fx-font-style: italic;");

        VBox titleBox = new VBox(4, title, subtitle, tipLabel, globalStatusLabel);

        Button settingsButton = new Button("Settings");
        settingsButton.setStyle(secondaryButtonStyle(uiSettings));
        settingsButton.setOnAction(event -> showSettingsPane());

        Button logoutButton = new Button("Logout");
        logoutButton.setStyle(primaryButtonStyle(uiSettings));
        logoutButton.setOnAction(event -> systemController.handleLogout());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(12, titleBox, spacer, settingsButton, logoutButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(24, 24, 16, 24));

        Label sectionLabel = new Label("Browse Games");
        sectionLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: " + (lightTheme ? "#334155" : "#CAD5E4") + "; -fx-font-weight: 700;");

        setupGamesPane(sectionLabel, uiSettings);
        setupModePane(uiSettings);
        setupLanPane(uiSettings);
        setupLeaderboardPane();
        setupSettingsPane(uiSettings);

        centerStack.getChildren().setAll(gamesPane, modePane, lanPane, leaderboardPane, settingsPane);
        showGamesPane();

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(centerStack);
        root.setStyle("-fx-background-color: " + appBackground(uiSettings) + ";");
        return root;
    }

    private void setupGamesPane(Label sectionLabel, UiSettings uiSettings) {
        FlowPane grid = new FlowPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.getChildren().addAll(
                createGameCard("Snake", "snake", uiSettings),
                createGameCard("Pong", "pong", uiSettings),
                createGameCard("Tic-Tac-Toe", "tic-tac-toe", uiSettings));

        Button leaderboardButton = new Button("View Leaderboard");
        leaderboardButton.setStyle(primaryButtonStyle(uiSettings));
        leaderboardButton.setOnAction(event -> showLeaderboardPane());

        gamesPane.getChildren().setAll(sectionLabel, grid, leaderboardButton);
        gamesPane.setPadding(new Insets(0, 24, 24, 24));
        gamesPane.setAlignment(Pos.TOP_LEFT);
    }

    private void setupModePane(UiSettings uiSettings) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        Label modeTitle = new Label("Mode Selection");
        modeTitle.setStyle("-fx-font-size: 24px; -fx-text-fill: " + (lightTheme ? "#1f2933" : "#E2E8F0") + "; -fx-font-weight: 800;");

        selectedGameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (lightTheme ? "#667085" : "#8EA0BF") + "; -fx-font-weight: 600;");

        Button singlePlayer = createModeButton("Single Player", uiSettings);
        Button localCoOp = createModeButton("Local Co-Op", uiSettings);
        Button lan = createModeButton("LAN Multiplayer", uiSettings);
        lan.setOnAction(event -> showLanChoicePane());

        Button backButton = new Button("Back");
        backButton.setStyle(secondaryButtonStyle(uiSettings));
        backButton.setOnAction(event -> showGamesPane());

        VBox card = new VBox(12, modeTitle, selectedGameLabel, singlePlayer, localCoOp, lan, backButton);
        card.setPadding(new Insets(24));
        card.setMaxWidth(360);
        card.setStyle("-fx-background-color: " + panelColor(uiSettings) + "; -fx-background-radius: 16;");
        card.setEffect(new DropShadow(16, Color.rgb(16, 24, 40, 0.10)));

        modePane.getChildren().setAll(card);
        modePane.setAlignment(Pos.TOP_CENTER);
        modePane.setPadding(new Insets(20, 24, 24, 24));
    }

    private void setupLanPane(UiSettings uiSettings) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        lanStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#A2B4D3") + "; -fx-font-weight: 600;");
        lanIpField.setPromptText("Host IP Address");
        lanIpField.setMaxWidth(260);
        lanIpField.setStyle(
                "-fx-background-color: " + panelColor(uiSettings) + "; -fx-text-fill: " + (lightTheme ? "#1F2937" : "#E2E8F0")
                        + "; -fx-prompt-text-fill: " + (lightTheme ? "#9CA3AF" : "#90A3C0") + ";"
                        + "-fx-font-size: 13px; -fx-background-radius: 10; -fx-padding: 9 12 9 12;");

        lanPane.setAlignment(Pos.TOP_CENTER);
        lanPane.setPadding(new Insets(20, 24, 24, 24));
        showLanChoicePane();
    }

    private void setupLeaderboardPane() {
        leaderboardPane.setAlignment(Pos.TOP_CENTER);
        leaderboardPane.setPadding(new Insets(20, 24, 24, 24));
        leaderboardPane.setVisible(false);
        leaderboardPane.setManaged(false);
    }

    private void setupSettingsPane(UiSettings uiSettings) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;

        Label title = new Label("Settings");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: " + (lightTheme ? "#1f2933" : "#E2E8F0") + ";");

        Label themeValue = new Label("Theme: " + uiSettings.getThemeMode().name());
        themeValue.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#90A3C0") + "; -fx-font-weight: 700;");

        Label accentValue = new Label("Accent: " + uiSettings.getAccentStyle().name());
        accentValue.setStyle(themeValue.getStyle());

        Label fpsValue = new Label("Show FPS: " + (uiSettings.isShowFps() ? "ON" : "OFF"));
        fpsValue.setStyle(themeValue.getStyle());

        Label ambientValue = new Label("Ambient Motion: " + (uiSettings.isAmbientMotion() ? "ON" : "OFF"));
        ambientValue.setStyle(themeValue.getStyle());

        Button themeButton = createModeButton("Toggle Theme", uiSettings);
        themeButton.setOnAction(event -> {
            UiSettings.ThemeMode next = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT
                    ? UiSettings.ThemeMode.DUSK
                    : UiSettings.ThemeMode.LIGHT;
            uiSettings.setThemeMode(next);
            reloadLobby();
        });

        Button accentButton = createModeButton("Cycle Accent", uiSettings);
        accentButton.setOnAction(event -> {
            UiSettings.AccentStyle next = switch (uiSettings.getAccentStyle()) {
                case SAGE -> UiSettings.AccentStyle.LAVENDER;
                case LAVENDER -> UiSettings.AccentStyle.CORAL;
                case CORAL -> UiSettings.AccentStyle.SAGE;
            };
            uiSettings.setAccentStyle(next);
            reloadLobby();
        });

        Button fpsButton = createModeButton("Toggle FPS Counter", uiSettings);
        fpsButton.setOnAction(event -> {
            uiSettings.setShowFps(!uiSettings.isShowFps());
            reloadLobby();
        });

        Button ambientButton = createModeButton("Toggle Ambient Motion", uiSettings);
        ambientButton.setOnAction(event -> {
            uiSettings.setAmbientMotion(!uiSettings.isAmbientMotion());
            reloadLobby();
        });

        Button backButton = new Button("Back");
        backButton.setStyle(secondaryButtonStyle(uiSettings));
        backButton.setOnAction(event -> showGamesPane());

        VBox card = new VBox(12, title, themeValue, accentValue, fpsValue, ambientValue, themeButton, accentButton, fpsButton, ambientButton, backButton);
        card.setPadding(new Insets(24));
        card.setMaxWidth(440);
        card.setStyle("-fx-background-color: " + panelColor(uiSettings) + "; -fx-background-radius: 16;");
        card.setEffect(new DropShadow(16, Color.rgb(16, 24, 40, 0.10)));

        settingsPane.getChildren().setAll(card);
        settingsPane.setAlignment(Pos.TOP_CENTER);
        settingsPane.setPadding(new Insets(20, 24, 24, 24));
    }

    private Button createModeButton(String modeLabel, UiSettings uiSettings) {
        Button button = new Button(modeLabel);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(primaryButtonStyle(uiSettings));
        button.setOnAction(event -> systemController.launchGame(selectedGameId, modeLabel));
        return button;
    }

    private Button createGameCard(String gameTitle, String gameId, UiSettings uiSettings) {
        Button card = new Button(gameTitle);
        card.setPrefSize(240, 170);
        card.setStyle(cardStyle(gameTitle, uiSettings, false));
        card.setEffect(new DropShadow(14, Color.rgb(16, 24, 40, 0.10)));
        card.setOnAction(event -> showModePane(gameTitle, gameId));
        card.setOnMouseEntered(event -> card.setStyle(cardStyle(gameTitle, uiSettings, true)));
        card.setOnMouseExited(event -> card.setStyle(cardStyle(gameTitle, uiSettings, false)));
        return card;
    }

    private String cardStyle(String gameTitle, UiSettings uiSettings, boolean hover) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        String tone = switch (gameTitle) {
            case "Snake" -> (lightTheme ? "#D8F2D2" : "#233723");
            case "Pong" -> (lightTheme ? "#D9EBFF" : "#1E3049");
            default -> (lightTheme ? "#F1E2FF" : "#312346");
        };
        String border = hover ? "-fx-border-color: #8EA0BF; -fx-border-width: 1;" : "";
        return "-fx-background-color: linear-gradient(to bottom right, " + panelColor(uiSettings) + ", " + tone + ");"
                + "-fx-text-fill: " + (lightTheme ? "#1f2933" : "#E2E8F0") + ";"
                + "-fx-font-size: 22px;"
                + "-fx-font-weight: 700;"
                + "-fx-background-radius: 16;"
                + "-fx-border-radius: 16;"
                + border
                + "-fx-padding: 12 12 12 12;";
    }

    private void showModePane(String gameTitle, String gameId) {
        selectedGameId = gameId;
        selectedGameTitle = gameTitle;
        selectedGameLabel.setText("Selected Game: " + gameTitle);
        gamesPane.setVisible(false);
        gamesPane.setManaged(false);
        modePane.setVisible(true);
        modePane.setManaged(true);
        lanPane.setVisible(false);
        lanPane.setManaged(false);
        settingsPane.setVisible(false);
        settingsPane.setManaged(false);
    }

    private void showGamesPane() {
        modePane.setVisible(false);
        modePane.setManaged(false);
        lanPane.setVisible(false);
        lanPane.setManaged(false);
        leaderboardPane.setVisible(false);
        leaderboardPane.setManaged(false);
        settingsPane.setVisible(false);
        settingsPane.setManaged(false);
        gamesPane.setVisible(true);
        gamesPane.setManaged(true);
    }

    private void showLanChoicePane() {
        UiSettings uiSettings = systemController.getUiSettings();
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        Label title = new Label("LAN Multiplayer");
        title.setStyle("-fx-font-size: 24px; -fx-text-fill: " + (lightTheme ? "#1f2933" : "#E2E8F0") + "; -fx-font-weight: 800;");

        Label gameLabel = new Label("Selected Game: " + (selectedGameTitle == null ? "-" : selectedGameTitle));
        gameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (lightTheme ? "#667085" : "#8EA0BF") + "; -fx-font-weight: 600;");

        Button hostButton = createLanActionButton("Host Game");
        hostButton.setOnAction(event -> showHostWaitingPane());

        Button joinButton = createLanActionButton("Join Game");
        joinButton.setOnAction(event -> showJoinPane());

        Button backButton = createSecondaryButton("Back");
        backButton.setOnAction(event -> showModeOnlyPane());

        VBox card = createLanCard(title, gameLabel, hostButton, joinButton, backButton);
        lanPane.getChildren().setAll(card);
        showLanOnlyPane();
    }

    private void showHostWaitingPane() {
        UiSettings uiSettings = systemController.getUiSettings();
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        String localIp = resolveLocalIp();
        Label title = new Label("Hosting LAN Match");
        title.setStyle("-fx-font-size: 24px; -fx-text-fill: " + (lightTheme ? "#1f2933" : "#E2E8F0") + "; -fx-font-weight: 800;");

        Label ipLabel = new Label("Share this IP: " + localIp);
        ipLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (lightTheme ? "#334155" : "#D1DBEA") + "; -fx-font-weight: 700;");

        Label waitingLabel = new Label("Waiting for player...");
        waitingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#A2B4D3") + "; -fx-font-weight: 600;");

        Button backButton = createSecondaryButton("Back");
        backButton.setOnAction(event -> showLanChoicePane());

        VBox card = createLanCard(title, ipLabel, waitingLabel, backButton);
        lanPane.getChildren().setAll(card);
        showLanOnlyPane();
        systemController.hostLanGame(selectedGameId);
    }

    private void showJoinPane() {
        UiSettings uiSettings = systemController.getUiSettings();
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        Label title = new Label("Join LAN Match");
        title.setStyle("-fx-font-size: 24px; -fx-text-fill: " + (lightTheme ? "#1f2933" : "#E2E8F0") + "; -fx-font-weight: 800;");

        lanStatusLabel.setText("Enter host IP to connect.");

        Button connectButton = createLanActionButton("Connect");
        connectButton.setOnAction(event -> {
            String ip = lanIpField.getText() == null ? "" : lanIpField.getText().trim();
            if (ip.isEmpty()) {
                lanStatusLabel.setText("Host IP address is required.");
                return;
            }
            lanStatusLabel.setText("Connecting to " + ip + "...");
            systemController.joinLanGame(selectedGameId, ip);
        });

        Button backButton = createSecondaryButton("Back");
        backButton.setOnAction(event -> showLanChoicePane());

        VBox card = createLanCard(title, lanIpField, lanStatusLabel, connectButton, backButton);
        lanPane.getChildren().setAll(card);
        showLanOnlyPane();
    }

    private VBox createLanCard(javafx.scene.Node... children) {
        UiSettings uiSettings = systemController.getUiSettings();
        VBox card = new VBox(12, children);
        card.setPadding(new Insets(24));
        card.setMaxWidth(420);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: " + panelColor(uiSettings) + "; -fx-background-radius: 16;");
        card.setEffect(new DropShadow(16, Color.rgb(16, 24, 40, 0.10)));
        return card;
    }

    private Button createLanActionButton(String label) {
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(primaryButtonStyle(systemController.getUiSettings()));
        return button;
    }

    private Button createSecondaryButton(String label) {
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(secondaryButtonStyle(systemController.getUiSettings()));
        return button;
    }

    private void showModeOnlyPane() {
        gamesPane.setVisible(false);
        gamesPane.setManaged(false);
        lanPane.setVisible(false);
        lanPane.setManaged(false);
        leaderboardPane.setVisible(false);
        leaderboardPane.setManaged(false);
        settingsPane.setVisible(false);
        settingsPane.setManaged(false);
        modePane.setVisible(true);
        modePane.setManaged(true);
    }

    private void showLanOnlyPane() {
        gamesPane.setVisible(false);
        gamesPane.setManaged(false);
        modePane.setVisible(false);
        modePane.setManaged(false);
        leaderboardPane.setVisible(false);
        leaderboardPane.setManaged(false);
        settingsPane.setVisible(false);
        settingsPane.setManaged(false);
        lanPane.setVisible(true);
        lanPane.setManaged(true);
    }

    private void showLeaderboardPane() {
        UiSettings uiSettings = systemController.getUiSettings();
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        Label title = new Label("Leaderboard");
        title.setStyle("-fx-font-size: 24px; -fx-text-fill: " + (lightTheme ? "#1f2933" : "#E2E8F0") + "; -fx-font-weight: 800;");

        VBox scoreRows = new VBox(8);
        List<String> topScores = systemController.getTopScores();
        if (topScores.isEmpty()) {
            Label empty = new Label("No scores yet. Play a game to set the first record.");
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#A2B4D3") + "; -fx-font-weight: 600;");
            scoreRows.getChildren().add(empty);
        } else {
            for (int index = 0; index < topScores.size(); index++) {
                scoreRows.getChildren().add(createLeaderboardRow(topScores.get(index), index));
            }
        }

        Button backButton = createSecondaryButton("Back");
        backButton.setOnAction(event -> showGamesPane());

        VBox card = new VBox(12, title, scoreRows, backButton);
        card.setPadding(new Insets(24));
        card.setMaxWidth(520);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle("-fx-background-color: " + panelColor(uiSettings) + "; -fx-background-radius: 16;");
        card.setEffect(new DropShadow(16, Color.rgb(16, 24, 40, 0.10)));

        leaderboardPane.getChildren().setAll(card);
        gamesPane.setVisible(false);
        gamesPane.setManaged(false);
        modePane.setVisible(false);
        modePane.setManaged(false);
        lanPane.setVisible(false);
        lanPane.setManaged(false);
        settingsPane.setVisible(false);
        settingsPane.setManaged(false);
        leaderboardPane.setVisible(true);
        leaderboardPane.setManaged(true);
    }

    private void showSettingsPane() {
        gamesPane.setVisible(false);
        gamesPane.setManaged(false);
        modePane.setVisible(false);
        modePane.setManaged(false);
        lanPane.setVisible(false);
        lanPane.setManaged(false);
        leaderboardPane.setVisible(false);
        leaderboardPane.setManaged(false);
        settingsPane.setVisible(true);
        settingsPane.setManaged(true);
    }

    private HBox createLeaderboardRow(String value, int index) {
        UiSettings uiSettings = systemController.getUiSettings();
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        Label rowLabel = new Label(value);
        rowLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (lightTheme ? "#334155" : "#D1DBEA") + "; -fx-font-weight: 600;");

        HBox row = new HBox(rowLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        String rowColor = index % 2 == 0
                ? (lightTheme ? "#F8FAFC" : "#1A2639")
                : (lightTheme ? "#EEF2F7" : "#182233");
        row.setStyle("-fx-background-color: " + rowColor + "; -fx-background-radius: 10;");
        return row;
    }

    private String pickTip() {
        return DAILY_TIPS[ThreadLocalRandom.current().nextInt(DAILY_TIPS.length)];
    }

    private String appBackground(UiSettings uiSettings) {
        if (uiSettings.getThemeMode() == UiSettings.ThemeMode.DUSK) {
            return "linear-gradient(to bottom right, #0F172A, #111A2A, #1A2240)";
        }
        return switch (uiSettings.getAccentStyle()) {
            case LAVENDER -> "linear-gradient(to bottom right, #F8F9FA, #F2F1FF, #F5F0FF)";
            case CORAL -> "linear-gradient(to bottom right, #F8F9FA, #FFF3EE, #FFECE4)";
            default -> "linear-gradient(to bottom right, #F8F9FA, #F2F7FF, #F5F3FF)";
        };
    }

    private String panelColor(UiSettings uiSettings) {
        return uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT ? "#FFFFFF" : "#111A2A";
    }

    private String primaryButtonStyle(UiSettings uiSettings) {
        String rightTone = switch (uiSettings.getAccentStyle()) {
            case LAVENDER -> "#D7C7F7";
            case CORAL -> "#FFC2B3";
            default -> "#C5E4C9";
        };
        return "-fx-background-color: linear-gradient(to right, #BEE9DA, " + rightTone + ");"
                + "-fx-text-fill: #1f2933; -fx-font-size: 14px;"
                + "-fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 11 18 11 18;";
    }

    private String secondaryButtonStyle(UiSettings uiSettings) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        return "-fx-background-color: " + (lightTheme ? "#E7EDF5" : "#243349") + ";"
                + "-fx-text-fill: " + (lightTheme ? "#1f2933" : "#D0D9E8") + "; -fx-font-size: 13px;"
                + "-fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 10 20 10 20;";
    }

    private void reloadLobby() {
        Scene scene = stage.getScene();
        if (scene == null) {
            stage.setScene(createScene());
            return;
        }
        scene.setRoot(createRoot());
    }

    private String resolveLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "Unavailable";
        }
    }
}
