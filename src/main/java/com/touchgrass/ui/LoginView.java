package com.touchgrass.ui;

import com.touchgrass.bl.SystemController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
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
        Label title = new Label("Touch Grass");
        title.setStyle("-fx-font-size: 36px; -fx-text-fill: #111827; -fx-font-weight: 800;");

        Label subtitle = new Label("Desktop Gaming Hub");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");

        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(320);
        usernameField.setStyle(inputStyle());

        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(320);
        passwordField.setStyle(inputStyle());

        Button loginButton = new Button("Login");
        loginButton.setStyle(primaryButtonStyle());
        loginButton.setOnAction(event -> attemptLogin());

        Button registerButton = new Button("Register");
        registerButton.setStyle(secondaryButtonStyle());
        registerButton.setOnAction(event -> showRegisterPane());

        feedbackLabel.setStyle("-fx-text-fill: #B42318; -fx-font-size: 12px; -fx-font-weight: 600;");

        HBox buttonRow = new HBox(12, loginButton, registerButton);
        buttonRow.setAlignment(Pos.CENTER);

        Button backToLoginButton = new Button("Back to Login");
        backToLoginButton.setStyle(secondaryButtonStyle());
        backToLoginButton.setOnAction(event -> showLoginPane());

        Button createAccountButton = new Button("Create Account");
        createAccountButton.setStyle(primaryButtonStyle());
        createAccountButton.setOnAction(event -> attemptRegistration());

        registerUsernameField.setPromptText("Username");
        registerUsernameField.setMaxWidth(320);
        registerUsernameField.setStyle(inputStyle());

        registerEmailField.setPromptText("Email");
        registerEmailField.setMaxWidth(320);
        registerEmailField.setStyle(inputStyle());

        registerPasswordField.setPromptText("Password");
        registerPasswordField.setMaxWidth(320);
        registerPasswordField.setStyle(inputStyle());

        registerConfirmPasswordField.setPromptText("Confirm Password");
        registerConfirmPasswordField.setMaxWidth(320);
        registerConfirmPasswordField.setStyle(inputStyle());

        registerFeedbackLabel.setStyle("-fx-text-fill: #B42318; -fx-font-size: 12px; -fx-font-weight: 600;");

        HBox registerButtonRow = new HBox(12, createAccountButton, backToLoginButton);
        registerButtonRow.setAlignment(Pos.CENTER);

        loginPane.getChildren().setAll(usernameField, passwordField, buttonRow, feedbackLabel);
        loginPane.setAlignment(Pos.CENTER);

        registerPane.getChildren().setAll(
                registerUsernameField,
                registerEmailField,
                registerPasswordField,
                registerConfirmPasswordField,
                registerButtonRow,
                registerFeedbackLabel);
        registerPane.setAlignment(Pos.CENTER);
        registerPane.setVisible(false);
        registerPane.setManaged(false);

        StackPane authStack = new StackPane(loginPane, registerPane);
        authStack.setPrefWidth(340);

        VBox card = new VBox(14, title, subtitle, authStack);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28));
        card.setMaxWidth(450);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16;");
        card.setEffect(new javafx.scene.effect.DropShadow(16, javafx.scene.paint.Color.rgb(16, 24, 40, 0.12)));

        VBox root = new VBox(card);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #F8F9FA, #EEF5FF, #F4F0FF);");

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

    private String inputStyle() {
        return "-fx-background-color: #F8FAFC;"
                + "-fx-text-fill: #1F2937;"
                + "-fx-prompt-text-fill: #94A3B8;"
                + "-fx-font-size: 14px;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 10 12 10 12;";
    }

    private String primaryButtonStyle() {
        return "-fx-background-color: linear-gradient(to right, #BDE7C5, #C6D7FF);"
                + "-fx-text-fill: #1F2937;"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: 700;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 10 24 10 24;";
    }

    private String secondaryButtonStyle() {
        return "-fx-background-color: #E6ECF3;"
                + "-fx-text-fill: #334155;"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: 600;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 10 24 10 24;";
    }
}
