package com.touchgrass.bl;

import com.touchgrass.bl.games.DriftTrackLogic;
import com.touchgrass.bl.games.DriftTrackState;
import com.touchgrass.bl.games.GameState;
import com.touchgrass.bl.games.InputCommand;
import com.touchgrass.bl.games.PongLogic;
import com.touchgrass.models.GameCatalog;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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
    private volatile DriftTrackState currentDriftTrackState;
    private final String engineId;
    private PongLogic pongLogic;
    private DriftTrackLogic driftTrackLogic;
    private String hostIpAddress;
    private int port;
    private Consumer<NetworkSession> onConnected;
    private Consumer<String> onError;

    public NetworkSession(String sessionId, String gameId, String mode) {
        super(sessionId, mode);
        this.ioLock = new Object();
        this.incomingCommands = new ConcurrentLinkedQueue<>();
        this.engineId = GameCatalog.resolveEngineId(gameId);
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
    }

    @Override
    public void end() {
        running = false;
        connected = false;
        closeQuietly(inputStream);
        closeQuietly(outputStream);
        closeQuietly(socket);
        closeQuietly(serverSocket);
    }

    @Override
    public void handleInput(InputCommand inputCommand, boolean pressed) {
        if (!pressed || inputCommand == null) {
            return;
        }
        if (!connected) {
            return;
        }
        if (host) {
            if (pongLogic != null) {
                pongLogic.processCommand(inputCommand, 1);
                return;
            }
            if (driftTrackLogic != null) {
                applyDriftCommand(driftTrackLogic, inputCommand, 1);
                return;
            }
        }
        sendCommand(inputCommand);
    }

    @Override
    public void tick() {
        if (host && connected) {
            if (pongLogic != null) {
                pongLogic.update();
                currentGameState = pongLogic.toGameState();
                sendObject(currentGameState);
                return;
            }
            if (driftTrackLogic != null) {
                driftTrackLogic.update();
                currentDriftTrackState = driftTrackLogic.toState();
                sendObject(currentDriftTrackState);
                return;
            }
        }
        syncState();
    }

    @Override
    public boolean isGameOver() {
        if (driftTrackLogic != null && host) {
            return driftTrackLogic.isGameOver();
        }
        if (currentDriftTrackState != null) {
            return currentDriftTrackState.isFinished();
        }
        return false;
    }

    @Override
    public int getScore() {
        if (driftTrackLogic != null && host) {
            return driftTrackLogic.getCombinedScore();
        }
        if (currentDriftTrackState != null) {
            return Math.max(currentDriftTrackState.getPlayer1Distance(), currentDriftTrackState.getPlayer2Distance());
        }
        return 0;
    }

    @Override
    public GameState getCurrentGameState() {
        return currentGameState;
    }

    @Override
    public DriftTrackState getDriftTrackState() {
        return currentDriftTrackState;
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
                if (driftTrackLogic != null) {
                    currentDriftTrackState = driftTrackLogic.toState();
                }
                notifyConnected();
                startHostCommandLoop();
            } catch (SocketException e) {
                handleNetworkFailure("Host socket error", e);
            } catch (IOException e) {
                handleNetworkFailure("Host connection failed", e);
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
            } catch (SocketException e) {
                handleNetworkFailure("Join socket error", e);
            } catch (IOException e) {
                handleNetworkFailure("Join connection failed", e);
            }
        });
    }

    public void sendCommand(InputCommand cmd) {
        sendObject(cmd);
    }

    public InputCommand pollIncomingCommand() {
        return incomingCommands.poll();
    }

    public void syncState() {
        while (incomingCommands.poll() != null) {
            // Drain queue when no dedicated state sync is used.
        }
    }

    public boolean isDriftTrackEngine() {
        return GameCatalog.ENGINE_DRIFT_TRACK.equalsIgnoreCase(engineId);
    }

    private void sendObject(Serializable payload) {
        if (!connected || outputStream == null || payload == null) {
            return;
        }
        synchronized (ioLock) {
            try {
                outputStream.reset();
                outputStream.writeObject(payload);
                outputStream.flush();
            } catch (SocketException e) {
                handleNetworkFailure("Unable to send payload", e);
            } catch (IOException e) {
                handleNetworkFailure("Unable to send payload", e);
            }
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
                    } else if (driftTrackLogic != null) {
                        applyDriftCommand(driftTrackLogic, command, 2);
                    } else {
                        incomingCommands.offer(command);
                    }
                }
            } catch (SocketException e) {
                if (running) {
                    handleNetworkFailure("Host command loop socket closed", e);
                }
                return;
            } catch (IOException | ClassNotFoundException e) {
                if (running) {
                    handleNetworkFailure("Host command loop stopped", e);
                }
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
                } else if (payload instanceof DriftTrackState driftState) {
                    currentDriftTrackState = driftState;
                } else if (payload instanceof InputCommand command) {
                    incomingCommands.offer(command);
                }
            } catch (SocketException e) {
                if (running) {
                    handleNetworkFailure("Client state loop socket closed", e);
                }
                return;
            } catch (IOException | ClassNotFoundException e) {
                if (running) {
                    handleNetworkFailure("Client state loop stopped", e);
                }
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
        System.err.println("Network Error: " + message);
        if (onError != null) {
            onError.accept(message);
        }
    }

    private void handleNetworkFailure(String context, Exception exception) {
        notifyError(context + ": " + exception.getMessage());
        end();
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
        if (GameCatalog.ENGINE_PONG.equalsIgnoreCase(engineId)) {
            pongLogic = new PongLogic();
            currentGameState = pongLogic.toGameState();
            return;
        }
        if (GameCatalog.ENGINE_DRIFT_TRACK.equalsIgnoreCase(engineId)) {
            driftTrackLogic = new DriftTrackLogic();
            currentDriftTrackState = driftTrackLogic.toState();
        }
    }

    private void applyDriftCommand(DriftTrackLogic logic, InputCommand command, int playerNumber) {
        if (command == InputCommand.LEFT) {
            logic.steerPlayerLeft(playerNumber);
        } else if (command == InputCommand.RIGHT) {
            logic.steerPlayerRight(playerNumber);
        }
    }
}
