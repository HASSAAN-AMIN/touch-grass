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

        primaryStage.setTitle("Touch Grass - Desktop Gaming Lounge");
        primaryStage.setScene(loginView.createScene());
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(600);
        primaryStage.setResizable(true);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
