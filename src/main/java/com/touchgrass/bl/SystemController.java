package com.touchgrass.bl;

public final class SystemController {
    private final AccountManager accountManager;
    private final GameFactory gameFactory;

    public SystemController() {
        this.accountManager = new AccountManager();
        this.gameFactory = new GameFactory();
    }

    public boolean handleLogin(String username, String password) {
        return accountManager.authenticate(username, password);
    }

    public void launchGame(String gameId, String mode) {
        Session session = gameFactory.createSession(gameId, mode);
        session.start();
    }
}
