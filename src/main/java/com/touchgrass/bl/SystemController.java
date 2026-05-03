package com.touchgrass.bl;

import com.touchgrass.ui.GameView;
import com.touchgrass.ui.MainLobbyView;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class SystemController {
    private final Stage stage;
    private final AccountManager accountManager;
    private final GameFactory gameFactory;
    private final LeaderboardManager leaderboardManager;
    private String currentUsername;

    public SystemController(Stage stage) {
        this.stage = stage;
        this.accountManager = new AccountManager();
        this.gameFactory = new GameFactory();
        this.leaderboardManager = new LeaderboardManager();
    }

    public boolean handleLogin(String username, String password) {
        boolean authenticated = accountManager.authenticate(username, password);
        if (authenticated) {
            currentUsername = username;
            return true;
        }
        currentUsername = null;
        return false;
    }

    public void launchGame(String gameId, String mode) {
        Session session = gameFactory.createSession(gameId, normalizeMode(mode));
        GameView gameView = new GameView(stage, this, gameId, session);
        Scene scene = stage.getScene();
        if (scene == null) {
            stage.setScene(gameView.createScene());
            scene = stage.getScene();
        } else {
            scene.setRoot(gameView.createRoot());
            gameView.bindToScene(scene);
        }
        session.start();
        gameView.startGameLoop();
    }

    public void handleGameOver(String gameId, int finalScore, boolean saveScore) {
        if (saveScore && currentUsername != null && !currentUsername.isBlank()) {
            boolean inserted = leaderboardManager.insertScore(currentUsername, gameId, finalScore);
            if (!inserted) {
                System.err.println("Score was not saved for user: " + currentUsername);
            }
        }

        MainLobbyView mainLobbyView = new MainLobbyView(stage, this);
        Scene scene = stage.getScene();
        if (scene == null) {
            stage.setScene(mainLobbyView.createScene());
            return;
        }
        scene.setRoot(mainLobbyView.createRoot());
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
