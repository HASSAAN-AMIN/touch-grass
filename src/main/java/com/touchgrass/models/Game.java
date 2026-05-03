package com.touchgrass.models;

public class Game {
    private String gameId;
    private String title;
    private boolean supportsMultiplayer;
    private boolean supportsNetwork;

    public Game(String gameId, String title, boolean supportsMultiplayer, boolean supportsNetwork) {
        this.gameId = gameId;
        this.title = title;
        this.supportsMultiplayer = supportsMultiplayer;
        this.supportsNetwork = supportsNetwork;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isSupportsMultiplayer() {
        return supportsMultiplayer;
    }

    public void setSupportsMultiplayer(boolean supportsMultiplayer) {
        this.supportsMultiplayer = supportsMultiplayer;
    }

    public boolean isSupportsNetwork() {
        return supportsNetwork;
    }

    public void setSupportsNetwork(boolean supportsNetwork) {
        this.supportsNetwork = supportsNetwork;
    }
}
