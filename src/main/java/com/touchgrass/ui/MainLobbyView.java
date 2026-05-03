package com.touchgrass.ui;

import com.touchgrass.bl.SystemController;
import com.touchgrass.bl.UiSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
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
            "Tip: LAN host controls authoritative Pong state.",
            "Tip: Save scores to keep your leaderboard streak alive.",
            "Tip: Toggle themes in settings for a different vibe."
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
        this.gamesPane = new VBox(18);
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
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: 900; -fx-text-fill: " + (lightTheme ? "#0F172A" : "#E2E8F0") + ";");
        Label subtitle = new Label("Modern Desktop Gaming Lounge");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#9FB1CD") + ";");
        Label tipLabel = new Label(pickTip());
        tipLabel.setStyle("-fx-font-size: 12px; -fx-font-style: italic; -fx-text-fill: " + (lightTheme ? "#667085" : "#8EA0BF") + ";");
        globalStatusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #C81E5B;");

        VBox titleBox = new VBox(4, title, subtitle, tipLabel, globalStatusLabel);

        Button leaderboardButton = new Button("Leaderboard");
        leaderboardButton.setStyle(secondaryButtonStyle(uiSettings));
        leaderboardButton.setOnAction(event -> showLeaderboardPane());

        Button settingsButton = new Button("Settings");
        settingsButton.setStyle(secondaryButtonStyle(uiSettings));
        settingsButton.setOnAction(event -> showSettingsPane());

        Button logoutButton = new Button("Logout");
        logoutButton.setStyle(primaryButtonStyle(uiSettings));
        logoutButton.setOnAction(event -> systemController.handleLogout());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(10, titleBox, spacer, leaderboardButton, settingsButton, logoutButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(24, 24, 14, 24));

        setupGamesPane(uiSettings);
        setupModePane(uiSettings);
        setupLanPane(uiSettings);
        setupLeaderboardPane(uiSettings);
        setupSettingsPane(uiSettings);

        centerStack.getChildren().setAll(gamesPane, modePane, lanPane, leaderboardPane, settingsPane);
        showGamesPane();

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(centerStack);
        root.setStyle("-fx-background-color: " + appBackground(uiSettings) + ";");
        return root;
    }

    private void setupGamesPane(UiSettings uiSettings) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        Label sectionTitle = new Label("Choose Your Game");
        sectionTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: " + (lightTheme ? "#1E293B" : "#E2E8F0") + ";");
        Label sectionSub = new Label("One game per row. Scroll and launch with style.");
        sectionSub.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#9FB1CD") + ";");

        VBox gameRows = new VBox(14,
                createGameRow("Snake", "snake", "Single player precision arcade", uiSettings),
                createGameRow("Pong", "pong", "Local or LAN competitive rally", uiSettings),
                createGameRow("Tic-Tac-Toe", "tic-tac-toe", "Classic strategy with modern visuals", uiSettings));

        ScrollPane scrollPane = new ScrollPane(gameRows);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox shellCard = new VBox(14, sectionTitle, sectionSub, scrollPane);
        shellCard.setPadding(new Insets(20));
        shellCard.setMaxWidth(860);
        shellCard.setPrefHeight(430);
        shellCard.setStyle("-fx-background-color: " + panelColor(uiSettings) + "; -fx-background-radius: 24;");
        shellCard.setEffect(new DropShadow(26, Color.rgb(15, 23, 42, 0.14)));

        gamesPane.getChildren().setAll(shellCard);
        gamesPane.setAlignment(Pos.TOP_CENTER);
        gamesPane.setPadding(new Insets(6, 24, 24, 24));
    }

    private HBox createGameRow(String title, String gameId, String description, UiSettings uiSettings) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: " + (lightTheme ? "#0F172A" : "#E2E8F0") + ";");

        Label descriptionLabel = new Label(description);
        descriptionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#9FB1CD") + ";");

        Label tag = new Label(gameId.equals("pong") ? "MULTIPLAYER READY" : "ARCADE");
        tag.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + (lightTheme ? "#334155" : "#D8E2F0") + ";"
                + "-fx-background-color: " + (lightTheme ? "#DFE9FB" : "#2B3D5A") + "; -fx-background-radius: 12; -fx-padding: 4 10 4 10;");

        VBox details = new VBox(8, titleLabel, descriptionLabel, tag);

        Button openButton = new Button("Select Mode");
        openButton.setStyle(primaryButtonStyle(uiSettings));
        openButton.setOnAction(event -> showModePane(title, gameId));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(16, details, spacer, openButton);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16, 18, 16, 18));
        row.setStyle("-fx-background-color: " + rowTone(gameId, uiSettings, false)
                + "; -fx-background-radius: 20; -fx-border-radius: 20;");
        row.setEffect(new DropShadow(14, Color.rgb(15, 23, 42, 0.10)));
        row.setOnMouseEntered(event -> row.setStyle("-fx-background-color: " + rowTone(gameId, uiSettings, true)
                + "; -fx-background-radius: 20; -fx-border-radius: 20;"));
        row.setOnMouseExited(event -> row.setStyle("-fx-background-color: " + rowTone(gameId, uiSettings, false)
                + "; -fx-background-radius: 20; -fx-border-radius: 20;"));
        return row;
    }

    private void setupModePane(UiSettings uiSettings) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;

        Label modeTitle = new Label("Pick Play Mode");
        modeTitle.setStyle("-fx-font-size: 26px; -fx-text-fill: " + (lightTheme ? "#1E293B" : "#E2E8F0") + "; -fx-font-weight: 800;");
        selectedGameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#9FB1CD") + "; -fx-font-weight: 700;");

        Button singlePlayer = createActionButton("Single Player", uiSettings, event -> systemController.launchGame(selectedGameId, "Single Player"));
        Button localCoOp = createActionButton("Local Co-Op", uiSettings, event -> systemController.launchGame(selectedGameId, "Local Co-Op"));
        Button lan = createActionButton("LAN Multiplayer", uiSettings, event -> showLanChoicePane());
        Button backButton = createActionButton("Back", uiSettings, event -> showGamesPane());
        backButton.setStyle(secondaryButtonStyle(uiSettings));

        VBox card = new VBox(12, modeTitle, selectedGameLabel, singlePlayer, localCoOp, lan, backButton);
        card.setPadding(new Insets(24));
        card.setMaxWidth(420);
        card.setStyle("-fx-background-color: " + panelColor(uiSettings) + "; -fx-background-radius: 22;");
        card.setEffect(new DropShadow(24, Color.rgb(15, 23, 42, 0.14)));

        modePane.getChildren().setAll(card);
        modePane.setAlignment(Pos.TOP_CENTER);
        modePane.setPadding(new Insets(20, 24, 24, 24));
    }

    private void setupLanPane(UiSettings uiSettings) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        lanStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#9FB1CD") + "; -fx-font-weight: 600;");
        lanIpField.setPromptText("Host IP Address");
        lanIpField.setMaxWidth(280);
        lanIpField.setStyle("-fx-background-color: " + (lightTheme ? "#EAF0FF" : "#1D2A42") + ";"
                + "-fx-text-fill: " + (lightTheme ? "#1E293B" : "#D6E1F0") + ";"
                + "-fx-prompt-text-fill: " + (lightTheme ? "#94A3B8" : "#89A1C3") + ";"
                + "-fx-font-size: 13px; -fx-background-radius: 14; -fx-padding: 10 12 10 12;");
        lanPane.setAlignment(Pos.TOP_CENTER);
        lanPane.setPadding(new Insets(20, 24, 24, 24));
        showLanChoicePane();
    }

    private void setupLeaderboardPane(UiSettings uiSettings) {
        leaderboardPane.setAlignment(Pos.TOP_CENTER);
        leaderboardPane.setPadding(new Insets(20, 24, 24, 24));
        leaderboardPane.setVisible(false);
        leaderboardPane.setManaged(false);
        showLeaderboardPane(uiSettings);
        leaderboardPane.setVisible(false);
        leaderboardPane.setManaged(false);
    }

    private void setupSettingsPane(UiSettings uiSettings) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;

        Label title = new Label("Design Settings");
        title.setStyle("-fx-font-size: 25px; -fx-font-weight: 800; -fx-text-fill: " + (lightTheme ? "#1E293B" : "#E2E8F0") + ";");
        Label subtitle = new Label("Personalize palette and in-game presentation");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#9FB1CD") + ";");

        Label themeValue = settingValue("Theme", uiSettings.getThemeMode().name(), lightTheme);
        Label accentValue = settingValue("Accent", uiSettings.getAccentStyle().name(), lightTheme);
        Label fpsValue = settingValue("FPS Counter", uiSettings.isShowFps() ? "On" : "Off", lightTheme);
        Label ambientValue = settingValue("Ambient Motion", uiSettings.isAmbientMotion() ? "On" : "Off", lightTheme);

        Button toggleTheme = createActionButton("Toggle Theme", uiSettings, event -> {
            uiSettings.setThemeMode(uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT
                    ? UiSettings.ThemeMode.DUSK
                    : UiSettings.ThemeMode.LIGHT);
            reloadLobby();
        });
        Button cycleAccent = createActionButton("Cycle Accent", uiSettings, event -> {
            UiSettings.AccentStyle next = switch (uiSettings.getAccentStyle()) {
                case SAGE -> UiSettings.AccentStyle.LAVENDER;
                case LAVENDER -> UiSettings.AccentStyle.CORAL;
                case CORAL -> UiSettings.AccentStyle.SAGE;
            };
            uiSettings.setAccentStyle(next);
            reloadLobby();
        });
        Button toggleFps = createActionButton("Toggle FPS", uiSettings, event -> {
            uiSettings.setShowFps(!uiSettings.isShowFps());
            reloadLobby();
        });
        Button toggleAmbient = createActionButton("Toggle Ambient Motion", uiSettings, event -> {
            uiSettings.setAmbientMotion(!uiSettings.isAmbientMotion());
            reloadLobby();
        });
        Button back = createActionButton("Back", uiSettings, event -> showGamesPane());
        back.setStyle(secondaryButtonStyle(uiSettings));

        VBox card = new VBox(12, title, subtitle, themeValue, accentValue, fpsValue, ambientValue,
                toggleTheme, cycleAccent, toggleFps, toggleAmbient, back);
        card.setPadding(new Insets(24));
        card.setMaxWidth(460);
        card.setStyle("-fx-background-color: " + panelColor(uiSettings) + "; -fx-background-radius: 22;");
        card.setEffect(new DropShadow(24, Color.rgb(15, 23, 42, 0.14)));

        settingsPane.getChildren().setAll(card);
        settingsPane.setAlignment(Pos.TOP_CENTER);
        settingsPane.setPadding(new Insets(20, 24, 24, 24));
    }

    private void showModePane(String gameTitle, String gameId) {
        selectedGameId = gameId;
        selectedGameTitle = gameTitle;
        selectedGameLabel.setText("Selected: " + gameTitle);
        showPane(modePane);
    }

    private void showGamesPane() {
        showPane(gamesPane);
    }

    private void showLanChoicePane() {
        UiSettings uiSettings = systemController.getUiSettings();
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;

        Label title = new Label("LAN Multiplayer");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: " + (lightTheme ? "#1E293B" : "#E2E8F0") + ";");
        Label gameLabel = new Label("Selected game: " + (selectedGameTitle == null ? "-" : selectedGameTitle));
        gameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#9FB1CD") + ";");

        Button hostButton = createActionButton("Host Game", uiSettings, event -> showHostWaitingPane());
        Button joinButton = createActionButton("Join Game", uiSettings, event -> showJoinPane());
        Button backButton = createActionButton("Back", uiSettings, event -> showModePane(selectedGameTitle, selectedGameId));
        backButton.setStyle(secondaryButtonStyle(uiSettings));

        VBox card = createLanCard(uiSettings, title, gameLabel, hostButton, joinButton, backButton);
        lanPane.getChildren().setAll(card);
        showPane(lanPane);
    }

    private void showHostWaitingPane() {
        UiSettings uiSettings = systemController.getUiSettings();
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;

        Label title = new Label("Hosting LAN Match");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: " + (lightTheme ? "#1E293B" : "#E2E8F0") + ";");
        Label ipLabel = new Label("Share this IP: " + resolveLocalIp());
        ipLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + (lightTheme ? "#334155" : "#CFDAEA") + ";");
        Label waiting = new Label("Waiting for player to connect...");
        waiting.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#9FB1CD") + ";");

        Button backButton = createActionButton("Back", uiSettings, event -> showLanChoicePane());
        backButton.setStyle(secondaryButtonStyle(uiSettings));

        VBox card = createLanCard(uiSettings, title, ipLabel, waiting, backButton);
        lanPane.getChildren().setAll(card);
        showPane(lanPane);
        systemController.hostLanGame(selectedGameId);
    }

    private void showJoinPane() {
        UiSettings uiSettings = systemController.getUiSettings();
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;

        Label title = new Label("Join LAN Match");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: " + (lightTheme ? "#1E293B" : "#E2E8F0") + ";");
        lanStatusLabel.setText("Enter host IP to connect.");

        Button connectButton = createActionButton("Connect", uiSettings, event -> {
            String ip = lanIpField.getText() == null ? "" : lanIpField.getText().trim();
            if (ip.isEmpty()) {
                lanStatusLabel.setText("Host IP address is required.");
                return;
            }
            lanStatusLabel.setText("Connecting to " + ip + "...");
            systemController.joinLanGame(selectedGameId, ip);
        });
        Button backButton = createActionButton("Back", uiSettings, event -> showLanChoicePane());
        backButton.setStyle(secondaryButtonStyle(uiSettings));

        VBox card = createLanCard(uiSettings, title, lanIpField, lanStatusLabel, connectButton, backButton);
        lanPane.getChildren().setAll(card);
        showPane(lanPane);
    }

    private VBox createLanCard(UiSettings uiSettings, javafx.scene.Node... children) {
        VBox card = new VBox(12, children);
        card.setPadding(new Insets(24));
        card.setMaxWidth(430);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: " + panelColor(uiSettings) + "; -fx-background-radius: 22;");
        card.setEffect(new DropShadow(24, Color.rgb(15, 23, 42, 0.14)));
        return card;
    }

    private void showLeaderboardPane() {
        showLeaderboardPane(systemController.getUiSettings());
        showPane(leaderboardPane);
    }

    private void showSettingsPane() {
        showPane(settingsPane);
    }

    private void showLeaderboardPane(UiSettings uiSettings) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        Label title = new Label("Leaderboard");
        title.setStyle("-fx-font-size: 25px; -fx-font-weight: 800; -fx-text-fill: " + (lightTheme ? "#1E293B" : "#E2E8F0") + ";");

        VBox scoreRows = new VBox(8);
        List<String> topScores = systemController.getTopScores();
        if (topScores.isEmpty()) {
            Label empty = new Label("No scores yet. Play and save your first run.");
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#9FB1CD") + ";");
            scoreRows.getChildren().add(empty);
        } else {
            for (int i = 0; i < topScores.size(); i++) {
                scoreRows.getChildren().add(createLeaderboardRow(topScores.get(i), i, lightTheme));
            }
        }

        ScrollPane listScroll = new ScrollPane(scoreRows);
        listScroll.setFitToWidth(true);
        listScroll.setPrefViewportHeight(290);
        listScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        listScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Button back = createActionButton("Back", uiSettings, event -> showGamesPane());
        back.setStyle(secondaryButtonStyle(uiSettings));

        VBox card = new VBox(12, title, listScroll, back);
        card.setPadding(new Insets(24));
        card.setMaxWidth(520);
        card.setStyle("-fx-background-color: " + panelColor(uiSettings) + "; -fx-background-radius: 22;");
        card.setEffect(new DropShadow(24, Color.rgb(15, 23, 42, 0.14)));
        leaderboardPane.getChildren().setAll(card);
    }

    private HBox createLeaderboardRow(String value, int index, boolean lightTheme) {
        Label label = new Label(value);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + (lightTheme ? "#334155" : "#D2DDED") + ";");

        HBox row = new HBox(label);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        String rowColor = index % 2 == 0
                ? (lightTheme ? "#EEF2FF" : "#1B2940")
                : (lightTheme ? "#E7EFFA" : "#182336");
        row.setStyle("-fx-background-color: " + rowColor + "; -fx-background-radius: 14;");
        return row;
    }

    private Label settingValue(String title, String value, boolean lightTheme) {
        Label label = new Label(title + ": " + value);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + (lightTheme ? "#475569" : "#B7C8E0") + ";");
        return label;
    }

    private Button createActionButton(String text, UiSettings uiSettings, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(primaryButtonStyle(uiSettings));
        button.setOnAction(action);
        return button;
    }

    private void showPane(VBox pane) {
        gamesPane.setVisible(false);
        gamesPane.setManaged(false);
        modePane.setVisible(false);
        modePane.setManaged(false);
        lanPane.setVisible(false);
        lanPane.setManaged(false);
        leaderboardPane.setVisible(false);
        leaderboardPane.setManaged(false);
        settingsPane.setVisible(false);
        settingsPane.setManaged(false);

        pane.setVisible(true);
        pane.setManaged(true);
    }

    private String rowTone(String gameId, UiSettings uiSettings, boolean hover) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        String tone = switch (gameId) {
            case "snake" -> (lightTheme ? "#E7F4EE" : "#223A32");
            case "pong" -> (lightTheme ? "#EAF1FD" : "#23344E");
            default -> (lightTheme ? "#F1EAFE" : "#2E2446");
        };
        if (!hover) {
            return tone;
        }
        return switch (gameId) {
            case "snake" -> (lightTheme ? "#DDEEE4" : "#2A4439");
            case "pong" -> (lightTheme ? "#DEE9FA" : "#2A3C59");
            default -> (lightTheme ? "#E8DDFB" : "#362A50");
        };
    }

    private String appBackground(UiSettings uiSettings) {
        if (uiSettings.getThemeMode() == UiSettings.ThemeMode.DUSK) {
            return "linear-gradient(to bottom right, #0B1324, #111B33, #1C2A4A)";
        }
        return switch (uiSettings.getAccentStyle()) {
            case LAVENDER -> "linear-gradient(to bottom right, #ECEFFA, #E5EAFD, #EFE5FF)";
            case CORAL -> "linear-gradient(to bottom right, #ECF0FA, #FDEDE4, #FEE8DF)";
            default -> "linear-gradient(to bottom right, #EEF2FA, #E7EEFF, #E7F4F0)";
        };
    }

    private String panelColor(UiSettings uiSettings) {
        return uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT
                ? "rgba(252,252,255,0.70)"
                : "rgba(20,30,48,0.84)";
    }

    private String primaryButtonStyle(UiSettings uiSettings) {
        String rightTone = switch (uiSettings.getAccentStyle()) {
            case LAVENDER -> "#D8C2FF";
            case CORAL -> "#FFBEA8";
            default -> "#BFD4FF";
        };
        return "-fx-background-color: linear-gradient(to right, #B9EACF, " + rightTone + ");"
                + "-fx-text-fill: #0F172A;"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: 800;"
                + "-fx-background-radius: 14;"
                + "-fx-padding: 10 16 10 16;";
    }

    private String secondaryButtonStyle(UiSettings uiSettings) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        return "-fx-background-color: " + (lightTheme ? "#DCE5F5" : "#253650") + ";"
                + "-fx-text-fill: " + (lightTheme ? "#334155" : "#D0D9E8") + ";"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: 700;"
                + "-fx-background-radius: 14;"
                + "-fx-padding: 10 16 10 16;";
    }

    private String pickTip() {
        return DAILY_TIPS[ThreadLocalRandom.current().nextInt(DAILY_TIPS.length)];
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
