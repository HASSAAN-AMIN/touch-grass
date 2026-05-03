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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class LoginView {
    private final Stage stage;
    private final SystemController systemController;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Label feedbackLabel;

    public LoginView(Stage stage, SystemController systemController) {
        this.stage = stage;
        this.systemController = systemController;
        this.usernameField = new TextField();
        this.passwordField = new PasswordField();
        this.feedbackLabel = new Label();
    }

    public Scene createScene() {
        Label title = new Label("Touch Grass");
        title.setStyle("-fx-font-size: 36px; -fx-text-fill: #f5f7ff; -fx-font-weight: 800;");

        Label subtitle = new Label("Desktop Gaming Hub");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #a8b0d6;");

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
        registerButton.setOnAction(event -> feedbackLabel.setText("Register flow coming soon."));

        feedbackLabel.setStyle("-fx-text-fill: #ffb4b4; -fx-font-size: 12px;");

        HBox buttonRow = new HBox(12, loginButton, registerButton);
        buttonRow.setAlignment(Pos.CENTER);

        VBox card = new VBox(14, title, subtitle, usernameField, passwordField, buttonRow, feedbackLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28));
        card.setMaxWidth(420);
        card.setStyle(
                "-fx-background-color: rgba(18, 22, 42, 0.9);"
                        + "-fx-border-color: rgba(122, 141, 255, 0.25);"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 16;"
                        + "-fx-background-radius: 16;");

        VBox root = new VBox(card);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0b0d19, #131a31, #1c2445);");

        return new Scene(root, 960, 600);
    }

    private void attemptLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        boolean authenticated = systemController.handleLogin(username, password);
        if (authenticated) {
            System.out.println("Login Success");
            MainLobbyView mainLobbyView = new MainLobbyView(stage, systemController);
            stage.setScene(mainLobbyView.createScene());
            return;
        }

        feedbackLabel.setText("Invalid username or password.");
    }

    private String inputStyle() {
        return "-fx-background-color: #242d52;"
                + "-fx-text-fill: #f5f7ff;"
                + "-fx-prompt-text-fill: #8e97c4;"
                + "-fx-font-size: 14px;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 10 12 10 12;";
    }

    private String primaryButtonStyle() {
        return "-fx-background-color: linear-gradient(to right, #4f6cff, #6a86ff);"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: 700;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 10 24 10 24;";
    }

    private String secondaryButtonStyle() {
        return "-fx-background-color: #2d355e;"
                + "-fx-text-fill: #d9deff;"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: 600;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 10 24 10 24;";
    }
}
