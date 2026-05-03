package com.touchgrass.bl;

public final class NetworkSession extends Session {
    private String hostIpAddress;
    private int port;

    public NetworkSession(String sessionId, String mode, String hostIpAddress, int port) {
        super(sessionId, mode);
        this.hostIpAddress = hostIpAddress;
        this.port = port;
    }

    public String getHostIpAddress() {
        return hostIpAddress;
    }

    public void setHostIpAddress(String hostIpAddress) {
        this.hostIpAddress = hostIpAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void start() {
        System.out.println("Network session started: " + getSessionId());
        syncState();
    }

    @Override
    public void end() {
        System.out.println("Network session ended: " + getSessionId());
    }

    @Override
    public void handleInput(String inputKey, boolean pressed) {
        System.out.println("Network input " + (pressed ? "pressed" : "released") + ": " + inputKey);
    }

    public void syncState() {
        System.out.println("Syncing network session state...");
    }
}
