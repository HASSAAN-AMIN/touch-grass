package com.touchgrass.bl.games;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class DriftTrackLogic {
    public static final int LANES_PER_SIDE = 3;
    public static final double TRACK_HEIGHT = 100.0;
    public static final double CAR_HEIGHT = 8.0;
    public static final int FINISH_DISTANCE = 1500;

    private final Random random = new Random();
    private final List<DriftTrackState.Obstacle> obstacles = new ArrayList<>();
    private int player1Lane = 1;
    private int player2Lane = 1;
    private int player1Distance;
    private int player2Distance;
    private boolean player1Crashed;
    private boolean player2Crashed;
    private int winningPlayer;
    private boolean finished;
    private int ticksSinceSpawn;
    private double scrollSpeed = 1.4;

    public synchronized void update() {
        if (finished) {
            return;
        }
        scrollSpeed = Math.min(3.2, scrollSpeed + 0.0010);

        if (!player1Crashed) {
            player1Distance += (int) Math.round(scrollSpeed * 1.4);
        }
        if (!player2Crashed) {
            player2Distance += (int) Math.round(scrollSpeed * 1.4);
        }

        ticksSinceSpawn++;
        int interval = Math.max(7, 18 - (int) (scrollSpeed * 2));
        if (ticksSinceSpawn >= interval) {
            spawnObstacleOnSide(0);
            spawnObstacleOnSide(1);
            ticksSinceSpawn = 0;
        }

        Iterator<DriftTrackState.Obstacle> iterator = obstacles.iterator();
        List<DriftTrackState.Obstacle> updated = new ArrayList<>();
        while (iterator.hasNext()) {
            DriftTrackState.Obstacle obstacle = iterator.next();
            double newY = obstacle.getY() + scrollSpeed;
            if (newY > TRACK_HEIGHT + CAR_HEIGHT) {
                continue;
            }
            DriftTrackState.Obstacle moved = new DriftTrackState.Obstacle(obstacle.getSide(), obstacle.getLane(), newY);
            updated.add(moved);
            if (collidesWithPlayer(moved)) {
                if (moved.getSide() == 0 && !player1Crashed) {
                    player1Crashed = true;
                } else if (moved.getSide() == 1 && !player2Crashed) {
                    player2Crashed = true;
                }
            }
        }
        obstacles.clear();
        obstacles.addAll(updated);

        evaluateOutcome();
    }

    public synchronized void steerPlayerLeft(int playerNumber) {
        if (finished) {
            return;
        }
        if (playerNumber == 1 && !player1Crashed) {
            player1Lane = Math.max(0, player1Lane - 1);
        } else if (playerNumber == 2 && !player2Crashed) {
            player2Lane = Math.max(0, player2Lane - 1);
        }
    }

    public synchronized void steerPlayerRight(int playerNumber) {
        if (finished) {
            return;
        }
        if (playerNumber == 1 && !player1Crashed) {
            player1Lane = Math.min(LANES_PER_SIDE - 1, player1Lane + 1);
        } else if (playerNumber == 2 && !player2Crashed) {
            player2Lane = Math.min(LANES_PER_SIDE - 1, player2Lane + 1);
        }
    }

    public synchronized DriftTrackState toState() {
        List<DriftTrackState.Obstacle> snapshot = new ArrayList<>();
        for (DriftTrackState.Obstacle obstacle : obstacles) {
            snapshot.add(new DriftTrackState.Obstacle(obstacle.getSide(), obstacle.getLane(), obstacle.getY()));
        }
        return new DriftTrackState(
                player1Lane,
                player2Lane,
                player1Distance,
                player2Distance,
                player1Crashed,
                player2Crashed,
                winningPlayer,
                finished,
                snapshot);
    }

    public synchronized boolean isGameOver() {
        return finished;
    }

    public synchronized int getWinningPlayer() {
        return winningPlayer;
    }

    public synchronized int getPlayer1Distance() {
        return player1Distance;
    }

    public synchronized int getPlayer2Distance() {
        return player2Distance;
    }

    public synchronized int getCombinedScore() {
        return Math.max(player1Distance, player2Distance);
    }

    private void evaluateOutcome() {
        if (player1Crashed && player2Crashed) {
            finished = true;
            winningPlayer = player1Distance >= player2Distance ? 1 : 2;
            return;
        }
        if (player1Distance >= FINISH_DISTANCE && !player1Crashed) {
            finished = true;
            winningPlayer = 1;
            return;
        }
        if (player2Distance >= FINISH_DISTANCE && !player2Crashed) {
            finished = true;
            winningPlayer = 2;
            return;
        }
        if (player1Crashed && !player2Crashed && player2Distance >= FINISH_DISTANCE / 2) {
            finished = true;
            winningPlayer = 2;
            return;
        }
        if (player2Crashed && !player1Crashed && player1Distance >= FINISH_DISTANCE / 2) {
            finished = true;
            winningPlayer = 1;
        }
    }

    private void spawnObstacleOnSide(int side) {
        int lane = random.nextInt(LANES_PER_SIDE);
        obstacles.add(new DriftTrackState.Obstacle(side, lane, -CAR_HEIGHT));
    }

    private boolean collidesWithPlayer(DriftTrackState.Obstacle obstacle) {
        double playerY = TRACK_HEIGHT - CAR_HEIGHT - 4;
        if (obstacle.getY() + CAR_HEIGHT < playerY || obstacle.getY() > playerY + CAR_HEIGHT) {
            return false;
        }
        if (obstacle.getSide() == 0) {
            return obstacle.getLane() == player1Lane;
        }
        return obstacle.getLane() == player2Lane;
    }
}
