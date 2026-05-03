package com.touchgrass.bl;

public final class LocalSession extends Session {
    private String p1Controls;
    private String p2Controls;

    public LocalSession(String sessionId, String mode, String p1Controls, String p2Controls) {
        super(sessionId, mode);
        this.p1Controls = p1Controls;
        this.p2Controls = p2Controls;
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
        processInput();
    }

    @Override
    public void end() {
        System.out.println("Local session ended: " + getSessionId());
    }

    @Override
    public void handleInput(String inputKey, boolean pressed) {
        System.out.println("Local input " + (pressed ? "pressed" : "released") + ": " + inputKey);
        processInput();
    }

    public void processInput() {
        System.out.println("Processing local split-keyboard input...");
    }
}
