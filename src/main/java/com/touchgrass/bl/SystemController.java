package com.touchgrass.bl;

import com.touchgrass.models.GameCatalog;
import com.touchgrass.ui.GameView;
import com.touchgrass.ui.LoginView;
import com.touchgrass.ui.MainLobbyView;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

public final class SystemController {
    private static final int LAN_PORT = 8080;
    private final Stage stage;
    private final AccountManager accountManager;
    private final GameFactory gameFactory;
    private final LeaderboardManager leaderboardManager;
    private final UiSettings uiSettings;
    private String currentUsername;
    private Session activeSession;
    private Consumer<String> statusMessageListener;

    public SystemController(Stage stage) {
        this.stage = stage;
        this.accountManager = new AccountManager();
        this.gameFactory = new GameFactory();
        this.leaderboardManager = new LeaderboardManager();
        this.uiSettings = new UiSettings();
    }

    public boolean handleLogin(String username, String password) {
        boolean authenticated = accountManager.authenticate(username, password);
        if (authenticated) {
            currentUsername = username;
            return true;
        }
        if (accountManager.getLastErrorMessage() != null) {
            notifyStatus(accountManager.getLastErrorMessage());
        }
        currentUsername = null;
        return false;
    }

    public String handleRegistration(String username, String email, String password, String confirmPassword) {
        if (password == null || !password.equals(confirmPassword)) {
            return "Passwords do not match.";
        }
        boolean registered = accountManager.registerUser(username, email, password);
        if (registered) {
            return null;
        }
        return accountManager.getLastErrorMessage() == null
                ? "Registration failed."
                : accountManager.getLastErrorMessage();
    }

    public void launchGame(String gameId, String mode) {
        String normalizedMode = normalizeMode(mode);
        if (!isModeSupported(gameId, normalizedMode)) {
            GameCatalog.GameDescriptor descriptor = GameCatalog.getById(gameId);
            notifyStatus(descriptor.title() + " does not support " + mode + ".");
            return;
        }
        Session session = gameFactory.createSession(gameId, normalizedMode);
        transitionToGameView(gameId, session);
        session.start();
    }

    public void hostLanGame(String gameId) {
        if (!isModeSupported(gameId, "LAN")) {
            notifyStatus(GameCatalog.getById(gameId).title() + " does not support LAN mode.");
            return;
        }
        Session session = gameFactory.createSession(gameId, "LAN");
        if (!(session instanceof NetworkSession networkSession)) {
            return;
        }
        networkSession.setHost(true);
        networkSession.setOnConnected(connectedSession -> Platform.runLater(() -> {
            transitionToGameView(gameId, connectedSession);
            connectedSession.start();
        }));
        networkSession.setOnError(this::notifyStatus);
        networkSession.hostGame(LAN_PORT);
    }

    public void joinLanGame(String gameId, String ipAddress) {
        if (!isModeSupported(gameId, "LAN")) {
            notifyStatus(GameCatalog.getById(gameId).title() + " does not support LAN mode.");
            return;
        }
        Session session = gameFactory.createSession(gameId, "LAN");
        if (!(session instanceof NetworkSession networkSession)) {
            return;
        }
        networkSession.setHost(false);
        networkSession.setOnConnected(connectedSession -> Platform.runLater(() -> {
            transitionToGameView(gameId, connectedSession);
            connectedSession.start();
        }));
        networkSession.setOnError(this::notifyStatus);
        networkSession.joinGame(ipAddress, LAN_PORT);
    }

    public void handleGameOver(String gameId, int finalScore, boolean saveScore) {
        String postTransitionMessage = null;
        if (saveScore && currentUsername != null && !currentUsername.isBlank()) {
            try {
                boolean inserted = leaderboardManager.insertScore(currentUsername, gameId, finalScore);
                if (!inserted) {
                    postTransitionMessage = "Unable to save score right now.";
                } else {
                    postTransitionMessage = "Score saved.";
                }
            } catch (RuntimeException e) {
                postTransitionMessage = "Unable to save score right now.";
                System.err.println("Database Error: " + e.getMessage());
            }
        } else if (saveScore) {
            postTransitionMessage = "Please log in before saving scores.";
        }

        MainLobbyView mainLobbyView = new MainLobbyView(stage, this);
        Scene scene = stage.getScene();
        if (scene == null) {
            stage.setScene(mainLobbyView.createScene());
            activeSession = null;
            if (postTransitionMessage != null) {
                notifyStatus(postTransitionMessage);
            }
            return;
        }
        scene.setRoot(mainLobbyView.createRoot());
        activeSession = null;
        if (postTransitionMessage != null) {
            notifyStatus(postTransitionMessage);
        }
    }

    public List<String> getTopScores(String gameId) {
        return leaderboardManager.getTopScores(gameId);
    }

    public void setStatusMessageListener(Consumer<String> statusMessageListener) {
        this.statusMessageListener = statusMessageListener;
    }

    public UiSettings getUiSettings() {
        return uiSettings;
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

    private boolean isModeSupported(String gameId, String normalizedMode) {
        GameCatalog.GameDescriptor descriptor = GameCatalog.getById(gameId);
        return switch (normalizedMode) {
            case "SinglePlayer" -> descriptor.supportsSinglePlayer();
            case "LocalCoOp" -> descriptor.supportsLocalCoOp();
            case "LAN" -> descriptor.supportsLan();
            default -> true;
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

    private void notifyStatus(String message) {
        if (statusMessageListener != null && message != null && !message.isBlank()) {
            Platform.runLater(() -> statusMessageListener.accept(message));
        }
    }
}
