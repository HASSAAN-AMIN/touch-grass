package com.touchgrass.bl;

import com.touchgrass.bl.games.InputCommand;
import com.touchgrass.bl.games.SnakeLogic;

public final class LocalSession extends Session {
    private final String gameId;
    private String p1Controls;
    private String p2Controls;
    private SnakeLogic snakeLogic;

    public LocalSession(String sessionId, String gameId, String mode, String p1Controls, String p2Controls) {
        super(sessionId, mode);
        this.gameId = gameId;
        this.p1Controls = p1Controls;
        this.p2Controls = p2Controls;
        initializeGameLogic();
    }

    public String getP1Controls() {
        return p1Controls;
    }

    public void setP1Controls(String p1Controls) {
        this.p1Controls = p1Controls;
    }

    public String getP2Controls() {
        return p2Controls;
    }

    public void setP2Controls(String p2Controls) {
        this.p2Controls = p2Controls;
    }

    @Override
    public void start() {
        System.out.println("Local session started: " + getSessionId());
    }

    @Override
    public void end() {
        System.out.println("Local session ended: " + getSessionId());
    }

    @Override
    public void handleInput(InputCommand inputCommand, boolean pressed) {
        if (snakeLogic != null && pressed) {
            snakeLogic.processCommand(inputCommand);
        }
    }

    @Override
    public void tick() {
        if (snakeLogic != null) {
            snakeLogic.update();
        }
    }

    public SnakeLogic getSnakeLogic() {
        return snakeLogic;
    }

    private void initializeGameLogic() {
        if ("snake".equalsIgnoreCase(gameId)) {
            snakeLogic = new SnakeLogic();
        }
    }
}
