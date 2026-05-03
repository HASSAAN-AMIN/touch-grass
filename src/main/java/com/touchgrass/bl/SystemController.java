package com.touchgrass.bl;

import com.touchgrass.ui.GameView;
import javafx.stage.Stage;

public final class SystemController {
    private final Stage stage;
    private final AccountManager accountManager;
    private final GameFactory gameFactory;

    public SystemController(Stage stage) {
        this.stage = stage;
        this.accountManager = new AccountManager();
        this.gameFactory = new GameFactory();
    }

    public boolean handleLogin(String username, String password) {
        return accountManager.authenticate(username, password);
    }

    public void launchGame(String gameId, String mode) {
        Session session = gameFactory.createSession(gameId, normalizeMode(mode));
        GameView gameView = new GameView(stage, this, session);
        stage.setScene(gameView.createScene());
        session.start();
        gameView.startGameLoop();
    }

    private String normalizeMode(String mode) {
        return switch (mode) {
            case "Single Player" -> "SinglePlayer";
            case "Local Co-Op" -> "LocalCoOp";
            case "LAN Multiplayer" -> "LAN";
            default -> mode;
        };
    }
}
