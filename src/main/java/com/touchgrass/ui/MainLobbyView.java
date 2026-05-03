package com.touchgrass.ui;

import com.touchgrass.bl.SystemController;
import com.touchgrass.bl.UiSettings;
import com.touchgrass.models.GameCatalog;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
            "Tip: Every game has a dedicated leaderboard.",
            "Tip: Snake variants share movement skills but have separate rankings.",
            "Tip: Use Local Co-Op for split-keyboard Pong sessions.",
            "Tip: LAN mode runs on port 8080 with host-authoritative sync.",
            "Tip: Toggle theme and accent in settings for quick visual tuning."
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
    private String leaderboardGameId;

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
        this.selectedGameId = GameCatalog.GAMES.get(0).gameId();
        this.selectedGameTitle = GameCatalog.GAMES.get(0).title();
        this.leaderboardGameId = this.selectedGameId;
    }

    public Scene createScene() {
        return new Scene(createRoot(), 960, 600);
    }

    public Parent createRoot() {
        UiSettings uiSettings = systemController.getUiSettings();
        boolean darkTheme = uiSettings.getThemeMode() != UiSettings.ThemeMode.LIGHT;

        systemController.setStatusMessageListener(message -> {
            lanStatusLabel.setText(message);
            globalStatusLabel.setText(message);
        });

        Label title = new Label("TOUCH GRASS");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: 900; -fx-letter-spacing: 2.8px; -fx-text-fill: " + primaryText(darkTheme) + ";");
        title.setEffect(new DropShadow(18, Color.web("#00E5FF", darkTheme ? 0.30 : 0.16)));

        Label subtitle = new Label("SCALABLE ARCADE HUB");
        subtitle.setStyle(overlineStyle(darkTheme));

        Label tipLabel = new Label(pickTip());
        tipLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + secondaryText(darkTheme) + ";");

        globalStatusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + danger(darkTheme) + ";");
        VBox titleBox = new VBox(4, title, subtitle, tipLabel, globalStatusLabel);

        Button leaderboardButton = createNavButton("LEADERBOARD", darkTheme, event -> showLeaderboardPane(leaderboardGameId));
        Button settingsButton = createNavButton("SETTINGS", darkTheme, event -> showSettingsPane());
        Button logoutButton = createNavButton("LOGOUT", darkTheme, event -> systemController.handleLogout());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(10, titleBox, spacer, leaderboardButton, settingsButton, logoutButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(20, 24, 14, 24));
        topBar.setStyle("-fx-background-color: " + topBarBg(darkTheme) + "; -fx-border-color: transparent transparent rgba(0,229,255,0.10) transparent; -fx-border-width: 0 0 1 0;");

        setupGamesPane(darkTheme);
        setupModePane(darkTheme);
        setupLanPane(darkTheme);
        setupLeaderboardPane(darkTheme);
        setupSettingsPane(darkTheme);

        centerStack.getChildren().setAll(gamesPane, modePane, lanPane, leaderboardPane, settingsPane);
        showGamesPane();

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(centerStack);
        root.setStyle("-fx-background-color: " + appBackground(darkTheme) + ";");
        return root;
    }

    private void setupGamesPane(boolean darkTheme) {
        Label overline = new Label("CHOOSE YOUR GAME");
        overline.setStyle(overlineStyle(darkTheme));

        Button featuredLeaderboard = createModeButton(
                "OPEN LEADERBOARD FOR " + GameCatalog.getById(leaderboardGameId).title().toUpperCase(),
                GameCatalog.resolveNeon(leaderboardGameId),
                darkTheme,
                event -> showLeaderboardPane(leaderboardGameId));

        VBox gameRows = new VBox(12);
        for (GameCatalog.GameDescriptor game : GameCatalog.GAMES) {
            gameRows.getChildren().add(createGameRow(game, darkTheme));
        }

        ScrollPane scrollPane = new ScrollPane(gameRows);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox shellCard = new VBox(12, overline, featuredLeaderboard, scrollPane);
        shellCard.setPadding(new Insets(18));
        shellCard.setMaxWidth(900);
        shellCard.setPrefHeight(448);
        shellCard.setStyle("-fx-background-color: " + cardSurface(darkTheme) + ";"
                + "-fx-background-radius: 24;"
                + "-fx-border-radius: 24;"
                + "-fx-border-width: 1.2;"
                + "-fx-border-color: " + borderColor(darkTheme) + ";");
        shellCard.setEffect(new DropShadow(30, Color.web("#00E5FF", darkTheme ? 0.16 : 0.10)));

        gamesPane.getChildren().setAll(shellCard);
        gamesPane.setAlignment(Pos.TOP_CENTER);
        gamesPane.setPadding(new Insets(8, 24, 24, 24));
    }

    private HBox createGameRow(GameCatalog.GameDescriptor game, boolean darkTheme) {
        String neon = game.neonColor();

        Label icon = new Label(game.icon());
        icon.setStyle("-fx-font-size: 38px;");
        icon.setEffect(new DropShadow(20, Color.web(neon, 0.65)));

        Label titleLabel = new Label(game.title());
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: " + primaryText(darkTheme) + ";");

        Label subtitleLabel = new Label(game.subtitle());
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + secondaryText(darkTheme) + ";");

        Label tag = new Label(game.engineId().toUpperCase().replace('-', ' '));
        tag.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-letter-spacing: 1.5px;"
                + "-fx-text-fill: " + neon + ";"
                + "-fx-background-color: transparent;"
                + "-fx-border-color: " + neon + ";"
                + "-fx-border-width: 1;"
                + "-fx-border-radius: 14;"
                + "-fx-padding: 3 10 3 10;");

        VBox details = new VBox(6, titleLabel, subtitleLabel, tag);

        Button playButton = createModeButton("SELECT MODE", neon, darkTheme, event -> showModePane(game.title(), game.gameId()));
        Button boardButton = createGhostButton("VIEW BOARD", darkTheme, event -> {
            leaderboardGameId = game.gameId();
            showLeaderboardPane(game.gameId());
        });

        VBox actions = new VBox(8, playButton, boardButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(14, icon, details, spacer, actions);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 16, 14, 14));
        row.setStyle(rowStyle(neon, darkTheme, false));
        row.setEffect(new DropShadow(28, Color.web(neon, 0.14)));
        row.setOnMouseEntered(event -> {
            row.setStyle(rowStyle(neon, darkTheme, true));
            row.setEffect(new DropShadow(34, Color.web(neon, 0.20)));
        });
        row.setOnMouseExited(event -> {
            row.setStyle(rowStyle(neon, darkTheme, false));
            row.setEffect(new DropShadow(28, Color.web(neon, 0.14)));
        });
        return row;
    }

    private void setupModePane(boolean darkTheme) {
        String neon = GameCatalog.resolveNeon(selectedGameId);
        Label overline = new Label("MODE SELECT");
        overline.setStyle(overlineStyle(darkTheme));

        Label heading = new Label("PICK A MODE");
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-letter-spacing: 2px; -fx-text-fill: " + primaryText(darkTheme) + ";");

        selectedGameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + secondaryText(darkTheme) + ";");

        Button singlePlayer = createModeButton("SINGLE PLAYER", neon, darkTheme, event -> systemController.launchGame(selectedGameId, "Single Player"));
        Button localCoOp = createModeButton("LOCAL CO-OP", neon, darkTheme, event -> systemController.launchGame(selectedGameId, "Local Co-Op"));
        Button lan = createModeButton("LAN MULTIPLAYER", gameNeon("pong"), darkTheme, event -> showLanChoicePane());
        Button viewBoard = createModeButton("VIEW LEADERBOARD", neon, darkTheme, event -> showLeaderboardPane(selectedGameId));
        Button back = createGhostButton("BACK", darkTheme, event -> showGamesPane());

        VBox card = new VBox(12, overline, heading, selectedGameLabel, singlePlayer, localCoOp, lan, viewBoard, back);
        card.setPadding(new Insets(24));
        card.setMaxWidth(440);
        card.setStyle("-fx-background-color: " + cardSurface(darkTheme) + ";"
                + "-fx-background-radius: 24;"
                + "-fx-border-radius: 24;"
                + "-fx-border-width: 1.2;"
                + "-fx-border-color: " + borderColor(darkTheme) + ";");
        card.setEffect(new DropShadow(30, Color.web(neon, 0.16)));

        modePane.getChildren().setAll(card);
        modePane.setAlignment(Pos.TOP_CENTER);
        modePane.setPadding(new Insets(20, 24, 24, 24));
    }

    private void setupLanPane(boolean darkTheme) {
        lanStatusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + secondaryText(darkTheme) + ";");
        lanIpField.setPromptText("HOST IP ADDRESS");
        lanIpField.setStyle("-fx-background-color: " + inputBg(darkTheme) + ";"
                + "-fx-text-fill: " + primaryText(darkTheme) + ";"
                + "-fx-prompt-text-fill: " + secondaryText(darkTheme) + ";"
                + "-fx-font-size: 13px;"
                + "-fx-border-color: " + gameNeon("pong") + ";"
                + "-fx-border-width: 1.1;"
                + "-fx-background-radius: 12;"
                + "-fx-border-radius: 12;"
                + "-fx-padding: 10 12 10 12;");
        lanIpField.setMaxWidth(280);

        lanPane.setAlignment(Pos.TOP_CENTER);
        lanPane.setPadding(new Insets(20, 24, 24, 24));
        showLanChoicePane();
    }

    private void setupLeaderboardPane(boolean darkTheme) {
        leaderboardPane.setAlignment(Pos.TOP_CENTER);
        leaderboardPane.setPadding(new Insets(20, 24, 24, 24));
        leaderboardPane.setVisible(false);
        leaderboardPane.setManaged(false);
        rebuildLeaderboardPane(darkTheme, leaderboardGameId);
    }

    private void rebuildLeaderboardPane(boolean darkTheme, String gameId) {
        GameCatalog.GameDescriptor game = GameCatalog.getById(gameId);
        String neon = game.neonColor();

        Label overline = new Label("TOP SCORES");
        overline.setStyle(overlineStyle(darkTheme));

        Label heading = new Label(game.title().toUpperCase() + " LEADERBOARD");
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-letter-spacing: 2px; -fx-text-fill: " + primaryText(darkTheme) + ";");

        HBox tabs = new HBox(6);
        tabs.setAlignment(Pos.CENTER_LEFT);
        for (GameCatalog.GameDescriptor descriptor : GameCatalog.GAMES) {
            Button tab = createModeButton(
                    descriptor.title().toUpperCase(),
                    descriptor.neonColor(),
                    darkTheme,
                    event -> {
                        leaderboardGameId = descriptor.gameId();
                        showLeaderboardPane(descriptor.gameId());
                    });
            tab.setStyle(modeButtonStyle(descriptor.neonColor(), darkTheme, descriptor.gameId().equals(leaderboardGameId)));
            tabs.getChildren().add(tab);
        }

        VBox rows = new VBox(8);
        List<String> topScores = systemController.getTopScores(gameId);
        if (topScores.isEmpty()) {
            Label empty = new Label("No scores recorded yet for " + game.title() + ".");
            empty.setStyle("-fx-font-size: 13px; -fx-text-fill: " + secondaryText(darkTheme) + ";");
            rows.getChildren().add(empty);
        } else {
            for (int i = 0; i < topScores.size(); i++) {
                rows.getChildren().add(createLeaderboardRow(topScores.get(i), i, darkTheme));
            }
        }

        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setPrefViewportHeight(250);

        Button back = createGhostButton("BACK", darkTheme, event -> showGamesPane());

        VBox card = new VBox(12, overline, heading, tabs, scroll, back);
        card.setPadding(new Insets(24));
        card.setMaxWidth(860);
        card.setStyle("-fx-background-color: " + cardSurface(darkTheme) + ";"
                + "-fx-background-radius: 24;"
                + "-fx-border-radius: 24;"
                + "-fx-border-width: 1.2;"
                + "-fx-border-color: " + borderColor(darkTheme) + ";");
        card.setEffect(new DropShadow(30, Color.web(neon, 0.16)));
        leaderboardPane.getChildren().setAll(card);
    }

    private HBox createLeaderboardRow(String value, int index, boolean darkTheme) {
        String medal = switch (index) {
            case 0 -> "🥇";
            case 1 -> "🥈";
            case 2 -> "🥉";
            default -> "•";
        };
        String leftNeon = switch (index) {
            case 0 -> gameNeon("snake");
            case 1 -> gameNeon("pong");
            case 2 -> gameNeon("tic-tac-toe");
            default -> borderColor(darkTheme);
        };

        Label medalLabel = new Label(medal);
        medalLabel.setStyle("-fx-font-size: 16px;");

        Label rowLabel = new Label(value);
        rowLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: " + primaryText(darkTheme) + ";");

        HBox row = new HBox(10, medalLabel, rowLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 10));
        row.setStyle("-fx-background-color: " + rowBg(darkTheme) + ";"
                + "-fx-border-color: transparent transparent transparent " + leftNeon + ";"
                + "-fx-border-width: 0 0 0 3;"
                + "-fx-background-radius: 14;"
                + "-fx-border-radius: 14;");
        return row;
    }

    private void setupSettingsPane(boolean darkTheme) {
        UiSettings settings = systemController.getUiSettings();
        String activeNeon = gameNeon("pong");

        Label overline = new Label("SYSTEM SETTINGS");
        overline.setStyle(overlineStyle(darkTheme));

        Label heading = new Label("VISUAL CONFIG");
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-letter-spacing: 2px; -fx-text-fill: " + primaryText(darkTheme) + ";");

        Label themeValue = createSettingValue("Theme: " + settings.getThemeMode().name(), darkTheme);
        Label accentValue = createSettingValue("Accent: " + settings.getAccentStyle().name(), darkTheme);
        Label fpsValue = createSettingValue("FPS Counter: " + (settings.isShowFps() ? "ON" : "OFF"), darkTheme);
        Label ambientValue = createSettingValue("Ambient Motion: " + (settings.isAmbientMotion() ? "ON" : "OFF"), darkTheme);

        Button toggleTheme = createModeButton("TOGGLE THEME", activeNeon, darkTheme, event -> {
            settings.setThemeMode(settings.getThemeMode() == UiSettings.ThemeMode.LIGHT
                    ? UiSettings.ThemeMode.DUSK
                    : UiSettings.ThemeMode.LIGHT);
            reloadLobby();
        });
        Button cycleAccent = createModeButton("CYCLE ACCENT", activeNeon, darkTheme, event -> {
            UiSettings.AccentStyle next = switch (settings.getAccentStyle()) {
                case SAGE -> UiSettings.AccentStyle.LAVENDER;
                case LAVENDER -> UiSettings.AccentStyle.CORAL;
                case CORAL -> UiSettings.AccentStyle.SAGE;
            };
            settings.setAccentStyle(next);
            reloadLobby();
        });
        Button toggleFps = createModeButton("TOGGLE FPS", activeNeon, darkTheme, event -> {
            settings.setShowFps(!settings.isShowFps());
            reloadLobby();
        });
        Button toggleAmbient = createModeButton("TOGGLE AMBIENT", activeNeon, darkTheme, event -> {
            settings.setAmbientMotion(!settings.isAmbientMotion());
            reloadLobby();
        });
        Button back = createGhostButton("BACK", darkTheme, event -> showGamesPane());

        VBox card = new VBox(12, overline, heading, themeValue, accentValue, fpsValue, ambientValue,
                toggleTheme, cycleAccent, toggleFps, toggleAmbient, back);
        card.setPadding(new Insets(24));
        card.setMaxWidth(460);
        card.setStyle("-fx-background-color: " + cardSurface(darkTheme) + ";"
                + "-fx-background-radius: 24;"
                + "-fx-border-radius: 24;"
                + "-fx-border-width: 1.2;"
                + "-fx-border-color: " + borderColor(darkTheme) + ";");
        card.setEffect(new DropShadow(30, Color.web(activeNeon, 0.16)));

        settingsPane.getChildren().setAll(card);
        settingsPane.setAlignment(Pos.TOP_CENTER);
        settingsPane.setPadding(new Insets(20, 24, 24, 24));
    }

    private Label createSettingValue(String text, boolean darkTheme) {
        Label value = new Label(text);
        value.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + secondaryText(darkTheme) + ";");
        return value;
    }

    private Button createNavButton(String label, boolean darkTheme, EventHandler<ActionEvent> action) {
        Button button = new Button(label);
        button.setStyle("-fx-background-color: " + topBarBg(darkTheme) + ";"
                + "-fx-text-fill: " + primaryText(darkTheme) + ";"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: 900;"
                + "-fx-letter-spacing: 1px;"
                + "-fx-border-color: rgba(0,184,255,0.20);"
                + "-fx-border-width: 1;"
                + "-fx-background-radius: 12;"
                + "-fx-border-radius: 12;"
                + "-fx-padding: 10 14 10 14;");
        button.setOnAction(action);
        return button;
    }

    private Button createModeButton(String label, String neon, boolean darkTheme, EventHandler<ActionEvent> action) {
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(modeButtonStyle(neon, darkTheme, false));
        button.setOnAction(action);
        button.setOnMouseEntered(event -> button.setStyle(modeButtonStyle(neon, darkTheme, true)));
        button.setOnMouseExited(event -> button.setStyle(modeButtonStyle(neon, darkTheme, false)));
        return button;
    }

    private Button createGhostButton(String label, boolean darkTheme, EventHandler<ActionEvent> action) {
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle("-fx-background-color: " + (darkTheme ? "rgba(10,18,40,0.68)" : "rgba(220,232,246,0.96)") + ";"
                + "-fx-text-fill: " + (darkTheme ? secondaryText(true) : "#2F4862") + ";"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: 800;"
                + "-fx-letter-spacing: 1px;"
                + "-fx-border-color: " + borderColor(darkTheme) + ";"
                + "-fx-border-width: 1;"
                + "-fx-background-radius: 12;"
                + "-fx-border-radius: 12;"
                + "-fx-padding: 10 14 10 14;");
        button.setOnAction(action);
        return button;
    }

    private void showModePane(String gameTitle, String gameId) {
        selectedGameId = gameId;
        selectedGameTitle = gameTitle;
        selectedGameLabel.setText("Selected game: " + gameTitle);
        leaderboardGameId = gameId;
        setupModePane(systemController.getUiSettings().getThemeMode() != UiSettings.ThemeMode.LIGHT);
        showPane(modePane);
    }

    private void showGamesPane() {
        showPane(gamesPane);
    }

    private void showLanChoicePane() {
        boolean darkTheme = systemController.getUiSettings().getThemeMode() != UiSettings.ThemeMode.LIGHT;
        Label overline = new Label("NETWORK SESSION");
        overline.setStyle(overlineStyle(darkTheme));

        Label title = new Label("LAN MULTIPLAYER");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-letter-spacing: 2px; -fx-text-fill: " + primaryText(darkTheme) + ";");

        Label game = new Label("Selected game: " + (selectedGameTitle == null ? "-" : selectedGameTitle));
        game.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + secondaryText(darkTheme) + ";");

        String neon = gameNeon("pong");
        Button host = createModeButton("HOST GAME", neon, darkTheme, event -> showHostWaitingPane());
        Button join = createModeButton("JOIN GAME", neon, darkTheme, event -> showJoinPane());
        Button back = createGhostButton("BACK", darkTheme, event -> showModePane(selectedGameTitle, selectedGameId));

        VBox card = new VBox(12, overline, title, game, host, join, back);
        card.setPadding(new Insets(24));
        card.setMaxWidth(440);
        card.setStyle("-fx-background-color: " + cardSurface(darkTheme) + ";"
                + "-fx-background-radius: 24;"
                + "-fx-border-radius: 24;"
                + "-fx-border-width: 1.2;"
                + "-fx-border-color: " + borderColor(darkTheme) + ";");
        card.setEffect(new DropShadow(30, Color.web(neon, 0.18)));

        lanPane.getChildren().setAll(card);
        showPane(lanPane);
    }

    private void showHostWaitingPane() {
        boolean darkTheme = systemController.getUiSettings().getThemeMode() != UiSettings.ThemeMode.LIGHT;
        String neon = gameNeon("pong");

        Label overline = new Label("LAN HOST");
        overline.setStyle(overlineStyle(darkTheme));

        Label title = new Label("WAITING FOR PLAYER");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-letter-spacing: 2px; -fx-text-fill: " + primaryText(darkTheme) + ";");

        Label ip = new Label("Host IP: " + resolveLocalIp());
        ip.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + gameNeon("pong") + ";");

        Label waiting = new Label("Server is live on port 8080");
        waiting.setStyle("-fx-font-size: 13px; -fx-text-fill: " + secondaryText(darkTheme) + ";");

        Button back = createGhostButton("BACK", darkTheme, event -> showLanChoicePane());

        VBox card = new VBox(12, overline, title, ip, waiting, back);
        card.setPadding(new Insets(24));
        card.setMaxWidth(440);
        card.setStyle("-fx-background-color: " + cardSurface(darkTheme) + ";"
                + "-fx-background-radius: 24;"
                + "-fx-border-radius: 24;"
                + "-fx-border-width: 1.2;"
                + "-fx-border-color: " + borderColor(darkTheme) + ";");
        card.setEffect(new DropShadow(30, Color.web(neon, 0.18)));

        lanPane.getChildren().setAll(card);
        showPane(lanPane);
        systemController.hostLanGame(selectedGameId);
    }

    private void showJoinPane() {
        boolean darkTheme = systemController.getUiSettings().getThemeMode() != UiSettings.ThemeMode.LIGHT;
        String neon = gameNeon("pong");
        lanStatusLabel.setText("Enter host IP to connect.");

        Label overline = new Label("LAN CLIENT");
        overline.setStyle(overlineStyle(darkTheme));

        Label title = new Label("JOIN HOST SESSION");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-letter-spacing: 2px; -fx-text-fill: " + primaryText(darkTheme) + ";");

        Button connect = createModeButton("CONNECT", neon, darkTheme, event -> {
            String ip = lanIpField.getText() == null ? "" : lanIpField.getText().trim();
            if (ip.isEmpty()) {
                lanStatusLabel.setText("Host IP address is required.");
                return;
            }
            lanStatusLabel.setText("Connecting to " + ip + "...");
            systemController.joinLanGame(selectedGameId, ip);
        });
        Button back = createGhostButton("BACK", darkTheme, event -> showLanChoicePane());

        VBox card = new VBox(12, overline, title, lanIpField, lanStatusLabel, connect, back);
        card.setPadding(new Insets(24));
        card.setMaxWidth(440);
        card.setStyle("-fx-background-color: " + cardSurface(darkTheme) + ";"
                + "-fx-background-radius: 24;"
                + "-fx-border-radius: 24;"
                + "-fx-border-width: 1.2;"
                + "-fx-border-color: " + borderColor(darkTheme) + ";");
        card.setEffect(new DropShadow(30, Color.web(neon, 0.18)));

        lanPane.getChildren().setAll(card);
        showPane(lanPane);
    }

    private void showLeaderboardPane(String gameId) {
        leaderboardGameId = gameId == null ? GameCatalog.GAMES.get(0).gameId() : gameId;
        boolean darkTheme = systemController.getUiSettings().getThemeMode() != UiSettings.ThemeMode.LIGHT;
        rebuildLeaderboardPane(darkTheme, leaderboardGameId);
        showPane(leaderboardPane);
    }

    private void showSettingsPane() {
        boolean darkTheme = systemController.getUiSettings().getThemeMode() != UiSettings.ThemeMode.LIGHT;
        setupSettingsPane(darkTheme);
        showPane(settingsPane);
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

    private String modeButtonStyle(String neon, boolean darkTheme, boolean hovered) {
        if (hovered) {
            return "-fx-background-color: " + neon + ";"
                    + "-fx-text-fill: #060D1C;"
                    + "-fx-font-size: 12px;"
                    + "-fx-font-weight: 900;"
                    + "-fx-letter-spacing: 1px;"
                    + "-fx-border-color: " + neon + ";"
                    + "-fx-border-width: 1.2;"
                    + "-fx-background-radius: 12;"
                    + "-fx-border-radius: 12;"
                    + "-fx-padding: 10 14 10 14;";
        }
        return "-fx-background-color: " + (darkTheme ? "rgba(8,14,30,0.84)" : "rgba(242,247,255,0.96)") + ";"
                + "-fx-text-fill: " + neon + ";"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: 900;"
                + "-fx-letter-spacing: 1px;"
                + "-fx-border-color: " + neon + ";"
                + "-fx-border-width: 1.2;"
                + "-fx-background-radius: 12;"
                + "-fx-border-radius: 12;"
                + "-fx-padding: 10 14 10 14;";
    }

    private String rowStyle(String neon, boolean darkTheme, boolean hovered) {
        return "-fx-background-color: " + (darkTheme ? "rgba(10,18,40,0.94)" : "rgba(245,249,255,0.96)") + ";"
                + "-fx-border-color: transparent transparent transparent " + neon + ";"
                + "-fx-border-width: 0 0 0 " + (hovered ? "4" : "3") + ";"
                + "-fx-background-radius: 16;"
                + "-fx-border-radius: 16;";
    }

    private String overlineStyle(boolean darkTheme) {
        return "-fx-font-size: 11px; -fx-font-weight: 900; -fx-letter-spacing: 2.5px; -fx-text-fill: " + secondaryText(darkTheme) + ";";
    }

    private String appBackground(boolean darkTheme) {
        return darkTheme
                ? "linear-gradient(to bottom right, #060D1C, #091633, #0F1B42)"
                : "linear-gradient(to bottom right, #F0FAF5, #EEF5FF, #F6FAFF)";
    }

    private String topBarBg(boolean darkTheme) {
        return darkTheme ? "rgba(6,13,28,0.97)" : "rgba(236,246,255,0.98)";
    }

    private String cardSurface(boolean darkTheme) {
        return darkTheme ? "rgba(10,18,40,0.94)" : "rgba(248,252,255,0.96)";
    }

    private String rowBg(boolean darkTheme) {
        return darkTheme ? "rgba(8,14,30,0.82)" : "rgba(234,242,252,0.96)";
    }

    private String inputBg(boolean darkTheme) {
        return darkTheme ? "#0F1D3A" : "#E8F0FC";
    }

    private String primaryText(boolean darkTheme) {
        return darkTheme ? "#E8F4FF" : "#0D1A36";
    }

    private String secondaryText(boolean darkTheme) {
        return darkTheme ? "#4A6A8A" : "#48627A";
    }

    private String borderColor(boolean darkTheme) {
        return darkTheme ? "rgba(0,184,255,0.22)" : "rgba(0,100,200,0.20)";
    }

    private String danger(boolean darkTheme) {
        return darkTheme ? "#FF4D8F" : "#CC1060";
    }

    private String gameNeon(String gameId) {
        return GameCatalog.resolveNeon(gameId);
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
