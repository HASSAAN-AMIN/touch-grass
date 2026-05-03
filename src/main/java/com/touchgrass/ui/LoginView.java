package com.touchgrass.ui;

import com.touchgrass.bl.SystemController;
import com.touchgrass.bl.UiSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public final class LoginView {
    private final Stage stage;
    private final SystemController systemController;
    private final TextField usernameField;
    private final TextField registerUsernameField;
    private final TextField registerEmailField;
    private final PasswordField passwordField;
    private final PasswordField registerPasswordField;
    private final PasswordField registerConfirmPasswordField;
    private final Label feedbackLabel;
    private final Label registerFeedbackLabel;
    private final VBox loginPane;
    private final VBox registerPane;

    public LoginView(Stage stage, SystemController systemController) {
        this.stage = stage;
        this.systemController = systemController;
        this.usernameField = new TextField();
        this.registerUsernameField = new TextField();
        this.registerEmailField = new TextField();
        this.passwordField = new PasswordField();
        this.registerPasswordField = new PasswordField();
        this.registerConfirmPasswordField = new PasswordField();
        this.feedbackLabel = new Label();
        this.registerFeedbackLabel = new Label();
        this.loginPane = new VBox(14);
        this.registerPane = new VBox(12);
    }

    public Scene createScene() {
        UiSettings uiSettings = systemController.getUiSettings();
        boolean darkTheme = uiSettings.getThemeMode() != UiSettings.ThemeMode.LIGHT;

        Label brandEmoji = new Label("🎮");
        brandEmoji.setStyle("-fx-font-size: 56px;");
        brandEmoji.setEffect(new DropShadow(20, Color.web(gameNeon("snake"), 0.65)));

        Label brandTitle = new Label("TOUCH GRASS");
        brandTitle.setStyle("-fx-font-size: 46px; -fx-font-weight: 900; -fx-letter-spacing: 3px; -fx-text-fill: " + primaryText(darkTheme) + ";");
        brandTitle.setEffect(new DropShadow(22, Color.web(gameNeon("snake"), 0.58)));

        Label brandSub = new Label("DARK NEON GAMING LOUNGE");
        brandSub.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-letter-spacing: 2.5px; -fx-text-fill: " + secondaryText(darkTheme) + ";");

        Label brandDescription = new Label("Built for local duels, LAN sessions, and score chasing in a premium arcade shell.");
        brandDescription.setWrapText(true);
        brandDescription.setStyle("-fx-font-size: 13px; -fx-text-fill: " + secondaryText(darkTheme) + ";");

        HBox chips = new HBox(8,
                createChip("JAVAFX 21", darkTheme),
                createChip("SOCKET LAN", darkTheme),
                createChip("LEADERBOARD", darkTheme));
        chips.setAlignment(Pos.CENTER_LEFT);

        VBox brandCard = new VBox(14, brandEmoji, brandTitle, brandSub, brandDescription, chips);
        brandCard.setPadding(new Insets(28));
        brandCard.setMaxWidth(470);
        brandCard.setStyle(
                "-fx-background-color: " + brandCardSurface(darkTheme) + ";"
                        + "-fx-background-radius: 24;"
                        + "-fx-border-radius: 24;"
                        + "-fx-border-width: 1.5;"
                        + "-fx-border-color: " + gameNeon("snake") + ";");
        brandCard.setEffect(new DropShadow(34, Color.web(gameNeon("snake"), 0.18)));

        Label authHeading = new Label("SIGN IN");
        authHeading.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-letter-spacing: 2px; -fx-text-fill: " + primaryText(darkTheme) + ";");

        Label authOverline = new Label("AUTHENTICATION");
        authOverline.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-letter-spacing: 2.5px; -fx-text-fill: " + secondaryText(darkTheme) + ";");

        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(Double.MAX_VALUE);
        usernameField.setStyle(inputStyle(darkTheme));

        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setStyle(inputStyle(darkTheme));

        Button loginButton = new Button("LOGIN");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setStyle(primaryButtonStyle());
        loginButton.setOnAction(event -> attemptLogin());

        Button registerButton = new Button("REGISTER");
        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setStyle(ghostButtonStyle(darkTheme));
        registerButton.setOnAction(event -> showRegisterPane());

        feedbackLabel.setStyle("-fx-text-fill: " + danger(darkTheme) + "; -fx-font-size: 12px; -fx-font-weight: 700;");

        HBox loginButtons = new HBox(10, loginButton, registerButton);
        loginButtons.setAlignment(Pos.CENTER);
        HBox.setHgrow(loginButton, Priority.ALWAYS);
        HBox.setHgrow(registerButton, Priority.ALWAYS);

        Button backToLoginButton = new Button("BACK");
        backToLoginButton.setMaxWidth(Double.MAX_VALUE);
        backToLoginButton.setStyle(ghostButtonStyle(darkTheme));
        backToLoginButton.setOnAction(event -> showLoginPane());

        Button createAccountButton = new Button("CREATE ACCOUNT");
        createAccountButton.setMaxWidth(Double.MAX_VALUE);
        createAccountButton.setStyle(primaryButtonStyle());
        createAccountButton.setOnAction(event -> attemptRegistration());

        registerUsernameField.setPromptText("Username");
        registerUsernameField.setMaxWidth(Double.MAX_VALUE);
        registerUsernameField.setStyle(inputStyle(darkTheme));

        registerEmailField.setPromptText("Email");
        registerEmailField.setMaxWidth(Double.MAX_VALUE);
        registerEmailField.setStyle(inputStyle(darkTheme));

        registerPasswordField.setPromptText("Password");
        registerPasswordField.setMaxWidth(Double.MAX_VALUE);
        registerPasswordField.setStyle(inputStyle(darkTheme));

        registerConfirmPasswordField.setPromptText("Confirm Password");
        registerConfirmPasswordField.setMaxWidth(Double.MAX_VALUE);
        registerConfirmPasswordField.setStyle(inputStyle(darkTheme));

        registerFeedbackLabel.setStyle("-fx-text-fill: " + danger(darkTheme) + "; -fx-font-size: 12px; -fx-font-weight: 700;");

        HBox registerButtons = new HBox(10, createAccountButton, backToLoginButton);
        registerButtons.setAlignment(Pos.CENTER);
        HBox.setHgrow(createAccountButton, Priority.ALWAYS);
        HBox.setHgrow(backToLoginButton, Priority.ALWAYS);

        loginPane.getChildren().setAll(usernameField, passwordField, loginButtons, feedbackLabel);
        loginPane.setAlignment(Pos.CENTER_LEFT);

        registerPane.getChildren().setAll(
                registerUsernameField,
                registerEmailField,
                registerPasswordField,
                registerConfirmPasswordField,
                registerButtons,
                registerFeedbackLabel);
        registerPane.setAlignment(Pos.CENTER_LEFT);
        registerPane.setVisible(false);
        registerPane.setManaged(false);

        StackPane authStack = new StackPane(loginPane, registerPane);
        authStack.setPrefWidth(350);
        authStack.setMaxWidth(Double.MAX_VALUE);

        VBox authCard = new VBox(14, authOverline, authHeading, authStack);
        authCard.setPadding(new Insets(24));
        authCard.setMaxWidth(420);
        authCard.setStyle(
                "-fx-background-color: " + cardSurface(darkTheme) + ";"
                        + "-fx-background-radius: 24;"
                        + "-fx-border-radius: 24;"
                        + "-fx-border-width: 1.5;"
                        + "-fx-border-color: " + borderColor(darkTheme) + ";");
        authCard.setEffect(new DropShadow(30, Color.web(gameNeon("pong"), 0.15)));

        HBox content = new HBox(24, brandCard, authCard);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(920);
        HBox.setHgrow(authCard, Priority.ALWAYS);

        VBox root = new VBox(content);
        root.setPadding(new Insets(28));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + appBackground(darkTheme) + ";");

        systemController.setStatusMessageListener(message -> {
            if (registerPane.isVisible()) {
                registerFeedbackLabel.setText(message);
            } else {
                feedbackLabel.setText(message);
            }
        });

        return new Scene(root, 960, 600);
    }

    private void attemptLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        boolean authenticated = systemController.handleLogin(username, password);
        if (authenticated) {
            MainLobbyView mainLobbyView = new MainLobbyView(stage, systemController);
            stage.setScene(mainLobbyView.createScene());
            return;
        }

        feedbackLabel.setText("Invalid username or password.");
    }

    private void attemptRegistration() {
        String username = registerUsernameField.getText();
        String email = registerEmailField.getText();
        String password = registerPasswordField.getText();
        String confirmPassword = registerConfirmPasswordField.getText();

        String result = systemController.handleRegistration(username, email, password, confirmPassword);
        if (result == null) {
            feedbackLabel.setStyle("-fx-text-fill: #00C25F; -fx-font-size: 12px; -fx-font-weight: 700;");
            feedbackLabel.setText("Registration successful. You can login now.");
            usernameField.setText(username);
            passwordField.clear();
            registerPasswordField.clear();
            registerConfirmPasswordField.clear();
            showLoginPane();
            return;
        }
        registerFeedbackLabel.setText(result);
    }

    private void showRegisterPane() {
        feedbackLabel.setText("");
        feedbackLabel.setStyle("-fx-text-fill: " + danger(systemController.getUiSettings().getThemeMode() != UiSettings.ThemeMode.LIGHT)
                + "; -fx-font-size: 12px; -fx-font-weight: 700;");
        loginPane.setVisible(false);
        loginPane.setManaged(false);
        registerPane.setVisible(true);
        registerPane.setManaged(true);
    }

    private void showLoginPane() {
        registerFeedbackLabel.setText("");
        loginPane.setVisible(true);
        loginPane.setManaged(true);
        registerPane.setVisible(false);
        registerPane.setManaged(false);
    }

    private Label createChip(String text, boolean darkTheme) {
        Label chip = new Label(text);
        chip.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-letter-spacing: 1.2px;"
                + "-fx-text-fill: " + gameNeon("pong") + ";"
                + "-fx-background-color: transparent;"
                + "-fx-border-color: " + gameNeon("pong") + ";"
                + "-fx-border-width: 1;"
                + "-fx-border-radius: 20;"
                + "-fx-background-radius: 20;"
                + "-fx-padding: 4 10 4 10;");
        chip.setEffect(new DropShadow(10, Color.web(gameNeon("pong"), darkTheme ? 0.22 : 0.14)));
        return chip;
    }

    private String inputStyle(boolean darkTheme) {
        return "-fx-background-color: " + (darkTheme ? "#0F1D3A" : "#EEF5FF") + ";"
                + "-fx-text-fill: " + primaryText(darkTheme) + ";"
                + "-fx-prompt-text-fill: " + secondaryText(darkTheme) + ";"
                + "-fx-font-size: 14px;"
                + "-fx-border-color: " + gameNeon("pong") + ";"
                + "-fx-border-width: 1.1;"
                + "-fx-background-radius: 12;"
                + "-fx-border-radius: 12;"
                + "-fx-padding: 11 12 11 12;";
    }

    private String primaryButtonStyle() {
        return "-fx-background-color: linear-gradient(to right, #00FF87, #6BFFB8);"
                + "-fx-text-fill: #060D1C;"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: 900;"
                + "-fx-letter-spacing: 1px;"
                + "-fx-background-radius: 13;"
                + "-fx-padding: 11 16 11 16;";
    }

    private String ghostButtonStyle(boolean darkTheme) {
        return "-fx-background-color: " + (darkTheme ? "rgba(10,18,40,0.68)" : "rgba(223,235,248,0.85)") + ";"
                + "-fx-text-fill: " + secondaryText(darkTheme) + ";"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: 800;"
                + "-fx-background-radius: 13;"
                + "-fx-border-radius: 13;"
                + "-fx-border-color: " + borderColor(darkTheme) + ";"
                + "-fx-border-width: 1;"
                + "-fx-padding: 11 16 11 16;";
    }

    private String appBackground(boolean darkTheme) {
        if (darkTheme) {
            return "linear-gradient(to bottom right, #060D1C, #091633, #0F1B42)";
        }
        return "linear-gradient(to bottom right, #F0FAF5, #EEF5FF, #EFF4FF)";
    }

    private String cardSurface(boolean darkTheme) {
        return darkTheme ? "rgba(10,18,40,0.94)" : "rgba(248,252,255,0.92)";
    }

    private String brandCardSurface(boolean darkTheme) {
        return darkTheme ? "rgba(8,18,38,0.72)" : "rgba(240,250,245,0.90)";
    }

    private String primaryText(boolean darkTheme) {
        return darkTheme ? "#E8F4FF" : "#0D1A36";
    }

    private String secondaryText(boolean darkTheme) {
        return darkTheme ? "#4A6A8A" : "#607080";
    }

    private String borderColor(boolean darkTheme) {
        return darkTheme ? "rgba(0,184,255,0.22)" : "rgba(0,100,200,0.16)";
    }

    private String danger(boolean darkTheme) {
        return darkTheme ? "#FF4D8F" : "#CC1060";
    }

    private String gameNeon(String gameId) {
        return switch (gameId) {
            case "snake" -> "#00FF87";
            case "pong" -> "#00E5FF";
            default -> "#B44FFF";
        };
    }
}
