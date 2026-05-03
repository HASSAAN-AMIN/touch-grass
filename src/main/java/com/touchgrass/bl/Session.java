package com.touchgrass.bl;

import com.touchgrass.bl.games.InputCommand;

public abstract class Session {
    private String sessionId;
    private String mode;

    protected Session(String sessionId, String mode) {
        this.sessionId = sessionId;
        this.mode = mode;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public abstract void start();

    public abstract void end();

    public abstract void handleInput(InputCommand inputCommand, boolean pressed);

    public abstract void tick();
}
