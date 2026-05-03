package com.touchgrass.bl;

import com.touchgrass.ui.GameView;
import com.touchgrass.ui.LoginView;
import com.touchgrass.ui.MainLobbyView;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

public final class SystemController {
    private static final int LAN_PORT = 8080;
    private final Stage stage;
    private final AccountManager accountManager;
    private final GameFactory gameFactory;
    private final LeaderboardManager leaderboardManager;
    private String currentUsername;
    private Session activeSession;

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
        transitionToGameView(gameId, session);
        session.start();
    }

    public void hostLanGame(String gameId) {
        Session session = gameFactory.createSession(gameId, "LAN");
        if (!(session instanceof NetworkSession networkSession)) {
            return;
        }
        networkSession.setHost(true);
        networkSession.setOnConnected(connectedSession -> Platform.runLater(() -> {
            transitionToGameView(gameId, connectedSession);
            connectedSession.start();
        }));
        networkSession.setOnError(message -> System.err.println("Host LAN error: " + message));
        networkSession.hostGame(LAN_PORT);
    }

    public void joinLanGame(String gameId, String ipAddress) {
        Session session = gameFactory.createSession(gameId, "LAN");
        if (!(session instanceof NetworkSession networkSession)) {
            return;
        }
        networkSession.setHost(false);
        networkSession.setOnConnected(connectedSession -> Platform.runLater(() -> {
            transitionToGameView(gameId, connectedSession);
            connectedSession.start();
        }));
        networkSession.setOnError(message -> System.err.println("Join LAN error: " + message));
        networkSession.joinGame(ipAddress, LAN_PORT);
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
            activeSession = null;
            return;
        }
        scene.setRoot(mainLobbyView.createRoot());
        activeSession = null;
    }

    public List<String> getTopScores() {
        return leaderboardManager.getTopScores();
    }

    public void handleLogout() {
        if (activeSession != null) {
            activeSession.end();
            activeSession = null;
        }
        currentUsername = null;

        LoginView loginView = new LoginView(stage, this);
        stage.setScene(loginView.createScene());
    }

    private String normalizeMode(String mode) {
        return switch (mode) {
            case "Single Player" -> "SinglePlayer";
            case "Local Co-Op" -> "LocalCoOp";
            case "LAN Multiplayer" -> "LAN";
            default -> mode;
        };
    }

    private void transitionToGameView(String gameId, Session session) {
        activeSession = session;
        GameView gameView = new GameView(stage, this, gameId, session);
        Scene scene = stage.getScene();
        if (scene == null) {
            stage.setScene(gameView.createScene());
            scene = stage.getScene();
        } else {
            scene.setRoot(gameView.createRoot());
            gameView.bindToScene(scene);
        }
        gameView.startGameLoop();
    }
}
