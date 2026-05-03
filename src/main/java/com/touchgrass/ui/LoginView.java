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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;

        Label heading = new Label("Welcome back");
        heading.setStyle("-fx-font-size: 32px; -fx-text-fill: " + (lightTheme ? "#0F172A" : "#E2E8F0") + "; -fx-font-weight: 800;");

        Label subtitle = new Label("Sign in to continue your gaming session");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (lightTheme ? "#64748B" : "#9FB1CD") + ";");

        Label brandTitle = new Label("Touch Grass");
        brandTitle.setStyle("-fx-font-size: 40px; -fx-font-weight: 900; -fx-text-fill: " + (lightTheme ? "#0F172A" : "#E2E8F0") + ";");
        Label brandTag = new Label("A premium desktop arcade lounge");
        brandTag.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: " + (lightTheme ? "#475569" : "#A8B8D3") + ";");
        Label brandDetail = new Label("Modern visuals, quick local play, and online duels.\nEverything in one clean space.");
        brandDetail.setStyle("-fx-font-size: 13px; -fx-line-spacing: 4; -fx-text-fill: " + (lightTheme ? "#667085" : "#8EA0BF") + ";");

        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(Double.MAX_VALUE);
        usernameField.setStyle(inputStyle(uiSettings));

        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setStyle(inputStyle(uiSettings));

        Button loginButton = new Button("Login");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setStyle(primaryButtonStyle(uiSettings));
        loginButton.setOnAction(event -> attemptLogin());

        Button registerButton = new Button("Register");
        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setStyle(secondaryButtonStyle(uiSettings));
        registerButton.setOnAction(event -> showRegisterPane());

        feedbackLabel.setStyle("-fx-text-fill: #B42318; -fx-font-size: 12px; -fx-font-weight: 600;");

        HBox buttonRow = new HBox(10, loginButton, registerButton);
        buttonRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(loginButton, Priority.ALWAYS);
        HBox.setHgrow(registerButton, Priority.ALWAYS);

        Button backToLoginButton = new Button("Back to Login");
        backToLoginButton.setMaxWidth(Double.MAX_VALUE);
        backToLoginButton.setStyle(secondaryButtonStyle(uiSettings));
        backToLoginButton.setOnAction(event -> showLoginPane());

        Button createAccountButton = new Button("Create Account");
        createAccountButton.setMaxWidth(Double.MAX_VALUE);
        createAccountButton.setStyle(primaryButtonStyle(uiSettings));
        createAccountButton.setOnAction(event -> attemptRegistration());

        registerUsernameField.setPromptText("Username");
        registerUsernameField.setMaxWidth(Double.MAX_VALUE);
        registerUsernameField.setStyle(inputStyle(uiSettings));

        registerEmailField.setPromptText("Email");
        registerEmailField.setMaxWidth(Double.MAX_VALUE);
        registerEmailField.setStyle(inputStyle(uiSettings));

        registerPasswordField.setPromptText("Password");
        registerPasswordField.setMaxWidth(Double.MAX_VALUE);
        registerPasswordField.setStyle(inputStyle(uiSettings));

        registerConfirmPasswordField.setPromptText("Confirm Password");
        registerConfirmPasswordField.setMaxWidth(Double.MAX_VALUE);
        registerConfirmPasswordField.setStyle(inputStyle(uiSettings));

        registerFeedbackLabel.setStyle("-fx-text-fill: #B42318; -fx-font-size: 12px; -fx-font-weight: 600;");

        HBox registerButtonRow = new HBox(10, createAccountButton, backToLoginButton);
        registerButtonRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(createAccountButton, Priority.ALWAYS);
        HBox.setHgrow(backToLoginButton, Priority.ALWAYS);

        loginPane.getChildren().setAll(usernameField, passwordField, buttonRow, feedbackLabel);
        loginPane.setAlignment(Pos.CENTER_LEFT);

        registerPane.getChildren().setAll(
                registerUsernameField,
                registerEmailField,
                registerPasswordField,
                registerConfirmPasswordField,
                registerButtonRow,
                registerFeedbackLabel);
        registerPane.setAlignment(Pos.CENTER_LEFT);
        registerPane.setVisible(false);
        registerPane.setManaged(false);

        StackPane authStack = new StackPane(loginPane, registerPane);
        authStack.setPrefWidth(360);
        authStack.setMaxWidth(Double.MAX_VALUE);

        VBox authCard = new VBox(16, heading, subtitle, authStack);
        authCard.setPadding(new Insets(28));
        authCard.setMaxWidth(430);
        authCard.setStyle("-fx-background-color: " + panelColor(lightTheme) + ";"
                + "-fx-background-radius: 24;"
                + "-fx-border-radius: 24;"
                + "-fx-border-color: " + (lightTheme ? "rgba(255,255,255,0.75)" : "rgba(255,255,255,0.10)") + ";"
                + "-fx-border-width: 1.2;");
        authCard.setEffect(new javafx.scene.effect.DropShadow(30, javafx.scene.paint.Color.rgb(15, 23, 42, 0.20)));

        VBox brandCard = new VBox(16, brandTitle, brandTag, brandDetail);
        brandCard.setPadding(new Insets(30));
        brandCard.setMaxWidth(420);
        brandCard.setStyle("-fx-background-color: " + glassTone(lightTheme) + ";"
                + "-fx-background-radius: 26;"
                + "-fx-border-radius: 26;"
                + "-fx-border-color: " + (lightTheme ? "rgba(255,255,255,0.60)" : "rgba(255,255,255,0.12)") + ";"
                + "-fx-border-width: 1.1;");
        brandCard.setEffect(new javafx.scene.effect.DropShadow(24, javafx.scene.paint.Color.rgb(15, 23, 42, 0.16)));

        HBox layout = new HBox(24, brandCard, authCard);
        layout.setAlignment(Pos.CENTER);
        layout.setMaxWidth(920);
        HBox.setHgrow(authCard, Priority.ALWAYS);

        VBox root = new VBox(layout);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: " + backgroundGradient(uiSettings) + ";");

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
            feedbackLabel.setStyle("-fx-text-fill: #027A48; -fx-font-size: 12px; -fx-font-weight: 600;");
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

    private String inputStyle(UiSettings uiSettings) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        return "-fx-background-color: " + (lightTheme ? "#EEF2FF" : "#1A2639") + ";"
                + "-fx-text-fill: " + (lightTheme ? "#1E293B" : "#E2E8F0") + ";"
                + "-fx-prompt-text-fill: " + (lightTheme ? "#94A3B8" : "#7B8DA8") + ";"
                + "-fx-font-size: 14px;"
                + "-fx-background-radius: 14;"
                + "-fx-padding: 12 14 12 14;";
    }

    private String primaryButtonStyle(UiSettings uiSettings) {
        String accentRight = switch (uiSettings.getAccentStyle()) {
            case LAVENDER -> "#D7C2FF";
            case CORAL -> "#FFBEA8";
            default -> "#BFD4FF";
        };
        return "-fx-background-color: linear-gradient(to right, #B8E9CF, " + accentRight + ");"
                + "-fx-text-fill: #0F172A;"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: 700;"
                + "-fx-background-radius: 14;"
                + "-fx-padding: 11 18 11 18;";
    }

    private String secondaryButtonStyle(UiSettings uiSettings) {
        boolean lightTheme = uiSettings.getThemeMode() == UiSettings.ThemeMode.LIGHT;
        return "-fx-background-color: " + (lightTheme ? "#DCE5F5" : "#22324A") + ";"
                + "-fx-text-fill: " + (lightTheme ? "#334155" : "#D0D9E8") + ";"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: 600;"
                + "-fx-background-radius: 14;"
                + "-fx-padding: 11 18 11 18;";
    }

    private String backgroundGradient(UiSettings uiSettings) {
        if (uiSettings.getThemeMode() == UiSettings.ThemeMode.DUSK) {
            return "linear-gradient(to bottom right, #0B1324, #111B33, #1C2A4A)";
        }
        return switch (uiSettings.getAccentStyle()) {
            case LAVENDER -> "linear-gradient(to bottom right, #EDEFFA, #E8EAFE, #EDE5FF)";
            case CORAL -> "linear-gradient(to bottom right, #EEF0FA, #FDEFE8, #FFE8DD)";
            default -> "linear-gradient(to bottom right, #EEF2FA, #E7EEFF, #E7F4F0)";
        };
    }

    private String panelColor(boolean lightTheme) {
        return lightTheme ? "rgba(252, 252, 255, 0.68)" : "rgba(20, 30, 48, 0.85)";
    }

    private String glassTone(boolean lightTheme) {
        return lightTheme ? "rgba(240, 247, 255, 0.54)" : "rgba(18, 28, 44, 0.70)";
    }
}
