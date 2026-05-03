package com.touchgrass.bl.games;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class CarRaceLogic {
    public static final int LANES = 4;
    public static final double TRACK_HEIGHT = 100.0;
    public static final double CAR_HEIGHT = 7.5;
    public static final double SPAWN_INTERVAL_TICKS = 12;
    public static final double MIN_SPAWN_INTERVAL_TICKS = 4;

    private final Random random = new Random();
    private final List<Traffic> traffic = new ArrayList<>();
    private int playerLane = 1;
    private int score;
    private int distance;
    private int ticksSinceSpawn;
    private double scrollSpeed = 1.6;
    private boolean gameOver;

    public synchronized void update() {
        if (gameOver) {
            return;
        }

        scrollSpeed = Math.min(3.6, scrollSpeed + 0.0014);
        distance++;
        score = distance;

        ticksSinceSpawn++;
        double interval = Math.max(MIN_SPAWN_INTERVAL_TICKS, SPAWN_INTERVAL_TICKS - (scrollSpeed * 1.4));
        if (ticksSinceSpawn >= interval) {
            spawnTraffic();
            ticksSinceSpawn = 0;
        }

        Iterator<Traffic> iterator = traffic.iterator();
        while (iterator.hasNext()) {
            Traffic car = iterator.next();
            car.y += scrollSpeed;
            if (car.y > TRACK_HEIGHT + CAR_HEIGHT) {
                iterator.remove();
                continue;
            }
            if (car.lane == playerLane && intersectsPlayer(car)) {
                gameOver = true;
                return;
            }
        }
    }

    public synchronized void steerLeft() {
        if (gameOver) {
            return;
        }
        playerLane = Math.max(0, playerLane - 1);
    }

    public synchronized void steerRight() {
        if (gameOver) {
            return;
        }
        playerLane = Math.min(LANES - 1, playerLane + 1);
    }

    public synchronized int getPlayerLane() {
        return playerLane;
    }

    public synchronized int getScore() {
        return score;
    }

    public synchronized int getDistance() {
        return distance;
    }

    public synchronized double getScrollSpeed() {
        return scrollSpeed;
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }

    public synchronized List<Traffic> getTraffic() {
        List<Traffic> snapshot = new ArrayList<>();
        for (Traffic car : traffic) {
            snapshot.add(new Traffic(car.lane, car.y));
        }
        return snapshot;
    }

    private void spawnTraffic() {
        int lane = random.nextInt(LANES);
        traffic.add(new Traffic(lane, -CAR_HEIGHT));
    }

    private boolean intersectsPlayer(Traffic car) {
        double playerY = TRACK_HEIGHT - CAR_HEIGHT - 6;
        return car.y + CAR_HEIGHT >= playerY && car.y <= playerY + CAR_HEIGHT;
    }

    public static final class Traffic {
        private final int lane;
        private double y;

        public Traffic(int lane, double y) {
            this.lane = lane;
            this.y = y;
        }

        public int lane() {
            return lane;
        }

        public double y() {
            return y;
        }
    }
}
