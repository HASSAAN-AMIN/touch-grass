package com.touchgrass;

import com.touchgrass.bl.SystemController;
import com.touchgrass.ui.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

public final class TouchGrassApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        SystemController systemController = new SystemController(primaryStage);
        LoginView loginView = new LoginView(primaryStage, systemController);

        primaryStage.setTitle("Touch Grass");
        primaryStage.setScene(loginView.createScene());
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
