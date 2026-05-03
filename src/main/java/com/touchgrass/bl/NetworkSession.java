package com.touchgrass.bl;

import com.touchgrass.bl.games.GameState;
import com.touchgrass.bl.games.InputCommand;
import com.touchgrass.bl.games.PongLogic;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public final class NetworkSession extends Session {
    private final Object ioLock;
    private final ConcurrentLinkedQueue<InputCommand> incomingCommands;
    private volatile boolean host;
    private volatile boolean connected;
    private volatile boolean running;
    private volatile Socket socket;
    private volatile ServerSocket serverSocket;
    private volatile ObjectOutputStream outputStream;
    private volatile ObjectInputStream inputStream;
    private volatile GameState currentGameState;
    private final String gameId;
    private PongLogic pongLogic;
    private String hostIpAddress;
    private int port;
    private Consumer<NetworkSession> onConnected;
    private Consumer<String> onError;

    public NetworkSession(String sessionId, String gameId, String mode) {
        super(sessionId, mode);
        this.ioLock = new Object();
        this.incomingCommands = new ConcurrentLinkedQueue<>();
        this.gameId = gameId;
        this.hostIpAddress = "127.0.0.1";
        this.port = 8080;
        initializeGameLogic();
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

    public boolean isHost() {
        return host;
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setOnConnected(Consumer<NetworkSession> onConnected) {
        this.onConnected = onConnected;
    }

    public void setOnError(Consumer<String> onError) {
        this.onError = onError;
    }

    @Override
    public void start() {
        running = true;
        System.out.println("Network session active: " + getSessionId());
    }

    @Override
    public void end() {
        running = false;
        connected = false;
        closeQuietly(inputStream);
        closeQuietly(outputStream);
        closeQuietly(socket);
        closeQuietly(serverSocket);
        System.out.println("Network session ended: " + getSessionId());
    }

    @Override
    public void handleInput(InputCommand inputCommand, boolean pressed) {
        if (!pressed || inputCommand == null) {
            return;
        }
        if (!connected) {
            return;
        }
        if (pongLogic != null && host) {
            pongLogic.processCommand(inputCommand, 1);
            return;
        }
        sendCommand(inputCommand);
    }

    @Override
    public void tick() {
        if (pongLogic != null && host && connected) {
            pongLogic.update();
            currentGameState = pongLogic.toGameState();
            sendState(currentGameState);
            return;
        }
        syncState();
    }

    @Override
    public boolean isGameOver() {
        return false;
    }

    @Override
    public int getScore() {
        return 0;
    }

    @Override
    public GameState getCurrentGameState() {
        return currentGameState;
    }

    public void hostGame(int port) {
        this.port = port;
        this.host = true;
        running = true;
        CompletableFuture.runAsync(() -> {
            try {
                serverSocket = new ServerSocket(port);
                socket = serverSocket.accept();
                initializeStreams(socket);
                if (pongLogic != null) {
                    currentGameState = pongLogic.toGameState();
                }
                notifyConnected();
                startHostCommandLoop();
            } catch (IOException e) {
                notifyError("Host connection failed: " + e.getMessage());
                end();
            }
        });
    }

    public void joinGame(String ipAddress, int port) {
        this.hostIpAddress = ipAddress;
        this.port = port;
        this.host = false;
        running = true;
        CompletableFuture.runAsync(() -> {
            try {
                socket = new Socket(ipAddress, port);
                initializeStreams(socket);
                notifyConnected();
                startClientStateLoop();
            } catch (IOException e) {
                notifyError("Join connection failed: " + e.getMessage());
                end();
            }
        });
    }

    public void sendCommand(InputCommand cmd) {
        if (!connected || outputStream == null) {
            return;
        }
        synchronized (ioLock) {
            try {
                outputStream.writeObject(cmd);
                outputStream.flush();
            } catch (IOException e) {
                notifyError("Unable to send command: " + e.getMessage());
            }
        }
    }

    public void sendState(GameState gameState) {
        if (!connected || outputStream == null || gameState == null) {
            return;
        }
        synchronized (ioLock) {
            try {
                outputStream.writeObject(gameState);
                outputStream.flush();
            } catch (IOException e) {
                notifyError("Unable to send game state: " + e.getMessage());
            }
        }
    }

    public InputCommand pollIncomingCommand() {
        return incomingCommands.poll();
    }

    public void syncState() {
        InputCommand received;
        while ((received = incomingCommands.poll()) != null) {
            System.out.println("Synced remote command: " + received);
        }
    }

    private void initializeStreams(Socket connectedSocket) throws IOException {
        outputStream = new ObjectOutputStream(connectedSocket.getOutputStream());
        outputStream.flush();
        inputStream = new ObjectInputStream(connectedSocket.getInputStream());
        connected = true;
    }

    private void startHostCommandLoop() {
        while (running && connected) {
            try {
                Object payload = inputStream.readObject();
                if (payload instanceof InputCommand command) {
                    if (pongLogic != null) {
                        pongLogic.processCommand(command, 2);
                    } else {
                        incomingCommands.offer(command);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                if (running) {
                    notifyError("Host command loop stopped: " + e.getMessage());
                }
                end();
                return;
            }
        }
    }

    private void startClientStateLoop() {
        while (running && connected) {
            try {
                Object payload = inputStream.readObject();
                if (payload instanceof GameState gameState) {
                    currentGameState = gameState;
                } else if (payload instanceof InputCommand command) {
                    incomingCommands.offer(command);
                }
            } catch (IOException | ClassNotFoundException e) {
                if (running) {
                    notifyError("Client state loop stopped: " + e.getMessage());
                }
                end();
                return;
            }
        }
    }

    private void notifyConnected() {
        if (onConnected != null) {
            onConnected.accept(this);
        }
    }

    private void notifyError(String message) {
        System.err.println(message);
        if (onError != null) {
            onError.accept(message);
        }
    }

    private void closeQuietly(Closeable resource) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (IOException ignored) {
            // Ignore close errors during shutdown.
        }
    }

    private void initializeGameLogic() {
        if ("pong".equalsIgnoreCase(gameId)) {
            pongLogic = new PongLogic();
            currentGameState = pongLogic.toGameState();
        }
    }
}
