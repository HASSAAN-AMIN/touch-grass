package com.touchgrass.bl;

import com.touchgrass.bl.games.DriftTrackState;
import com.touchgrass.bl.games.GameState;
import com.touchgrass.bl.games.InputCommand;

public abstract class Session {
    private final String sessionId;
    private final String mode;

    protected Session(String sessionId, String mode) {
        this.sessionId = sessionId;
        this.mode = mode;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getMode() {
        return mode;
    }

    public abstract void start();

    public abstract void end();

    public abstract void handleInput(InputCommand inputCommand, boolean pressed);

    public abstract void tick();

    public abstract boolean isGameOver();

    public abstract int getScore();

    public GameState getCurrentGameState() {
        return null;
    }

    public DriftTrackState getDriftTrackState() {
        return null;
    }
}
